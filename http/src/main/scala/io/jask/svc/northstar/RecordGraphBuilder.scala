package io.jask.svc.northstar

import java.util.UUID

import akka.actor.ActorSystem
import akka.kafka.ProducerMessage
import akka.kafka.scaladsl.Producer
import akka.stream.scaladsl.{Framing, Keep, RunnableGraph, Sink, Source}
import akka.util.ByteString
import io.jask.dto.northstar.UploadStat
import org.apache.kafka.clients.producer.ProducerRecord

import scala.concurrent.{ExecutionContext, Future}

/** Creates runnable graphs that can be used to handle line-oriented raw record streams.
  *
  * The graphs constructed by this builder accept a byte string source and perform the following
  * transformations:
  *
  *  - Chop the bytes into "\n"-delimited lines.
  *  - Transform each line into a kafka message, keyed on upload UUID
  *  - Send the message to kafka
  *  - Fold up the results into a stats object, useful for sending back to the requestor.
  *
  * @param ctx Application-wide variables
  */
class RecordGraphBuilder(ctx: IngestContext) extends GraphBuilder[UploadStat] {
  private[this] implicit val system: ActorSystem  = ctx.system
  private[this] implicit val ec: ExecutionContext = ctx.system.dispatcher

  /** Graph stage that produces messages to Kafka */
  private[this] val producerFlow =
    Producer.flow[UUID, Array[Byte], UploadStat](ctx.producerSettings, ctx.producer)

  /** Graph stage that breaks a byte string into line-delimited messages. */
  private[this] val framer =
    Framing.delimiter(ByteString("\n"), maximumFrameLength = 100000, allowTruncation = true)

  /** Graph sink that folds up kafka upload responses into a single UploadStat DTO. */
  private[this] val uploadStatSink: Sink[ProducerMessage.Result[UUID, Array[Byte], UploadStat],
                                         Future[UploadStat]] =
    Sink.fold(UploadStat()) { (acc, result) =>
      val stat = result.message.passThrough

      acc.copy(count = acc.count + 1,
               size = acc.size + stat.size,
               failures = acc.failures + stat.failures,
               maxRecord = math.max(acc.maxRecord, stat.maxRecord))
    }

  /** Creates a graph stage that translates bytes to kafka messages.
    *
    * @param id       The key for every kafka message.
    * @param dataType The data type (dictates how the messages are wrapped)
    * @return A graph stage that uploads messages with the given ID and type to kafka.
    */
  private[this] def messageCreator(id: UUID, dataType: String /* TODO! */ ) = {
    framer.map { line: ByteString =>
      val stat = UploadStat(count = 1, size = line.size, maxRecord = line.size)
      val rec  = new ProducerRecord(ctx.topic, id, line.toArray)

      ProducerMessage.Message(rec, stat)
    }
  }

  override def makeGraph(source: Source[ByteString, _],
                         dataType: String, /* TODO */
                         id: UUID) = {
    source
      .viaMat(messageCreator(id, dataType))(Keep.none)
      .viaMat(producerFlow)(Keep.none)
      .toMat(uploadStatSink)(Keep.right)
  }
}
