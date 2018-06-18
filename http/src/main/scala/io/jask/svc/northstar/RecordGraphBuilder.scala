package io.jask.svc.northstar

import java.util.UUID

import akka.actor.ActorSystem
import akka.kafka.ProducerMessage
import akka.kafka.scaladsl.Producer
import akka.stream.scaladsl.{Flow, Framing, Keep, RunnableGraph, Sink, Source}
import akka.util.ByteString
import io.circe.{Json, ParsingFailure}
import io.jask.dto.northstar.{Envelope, JsonSupport, UploadStat}
import org.apache.kafka.clients.producer.ProducerRecord
import io.circe.jawn._
import org.slf4j.LoggerFactory

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
class RecordGraphBuilder(ctx: IngestContext) extends GraphBuilder[UploadStat] with JsonSupport {
  private[this] implicit val system: ActorSystem      = ctx.system
  private[this] implicit val ec    : ExecutionContext = ctx.system.dispatcher

  private[this] val log = LoggerFactory.getLogger(this.getClass)

  /** Graph stage that produces messages to Kafka */
  private[this] val producerFlow =
    Producer.flow[UUID, Array[Byte], UploadStat](ctx.producerSettings, ctx.producer)

  /** Graph stage that breaks a byte string into line-delimited messages. */
  private[this] val framer =
    Framing.delimiter(ByteString("\n"), maximumFrameLength = 100000, allowTruncation = true)

  /** Graph sink that folds up kafka upload responses into a single UploadStat DTO. */
  private[this] type UploadStatSink =
    Sink[ProducerMessage.Result[UUID, Array[Byte], UploadStat], Future[UploadStat]]

  private[this] def uploadStatSink(fileId: UUID): UploadStatSink = {
    Sink.fold(UploadStat(id = fileId)) { (acc, result) =>
      val stat = result.message.passThrough

      acc.copy(goodRecords = acc.goodRecords + 1,
               size = acc.size + stat.size,
               badRecords = acc.badRecords + stat.badRecords,
               maxRecord = math.max(acc.maxRecord, stat.maxRecord))
    }
  }

  /** A flow stage that takes raw bytes, emits parsed JSON and counts parse failures.
    *
    * It makes use of the framer (above) for splitting input bytes into lines.
    *
    * Emits (Json: parsed, Long: upload index, Int: message size)
    * Materializes Future[Long]: Number of lines that were invalid JSON.
    */
  private[this] val jsonParserFlow: Flow[ByteString, (Json, Long, Int), Future[Long]] = {
    // A sink that counts failures
    val failureCounter = Sink.fold(0L)((acc: Long, _: Any) => acc + 1)

    // A filter that diverts parse failures to the failure counter sink.
    val filter: ((Either[Any, Any], Long, Int)) => Boolean = {
      case ((e, _, _)) => {
        e.isLeft
      }
    }

    framer.zipWithIndex
      .map {
        case (bytes: ByteString, idx: Long) => {
          (parse(bytes.decodeString("UTF-8")), idx, bytes.size)
        }
      }
      .divertToMat(failureCounter, filter)(Keep.right)
      .map {
        case (parsed, idx, size) => {
          (parsed.right.get, idx, size)
        }
      }
  }

  /** Creates a graph stage that translates bytes to kafka messages.
    *
    * @param id       The key for every kafka message.
    * @param dataType The data type (dictates how the messages are wrapped)
    * @return A graph stage that uploads messages with the given ID and type to kafka.
    */
  private[this] def messageCreator(id: UUID, dataType: String /* TODO! */)

  = {
    jsonParserFlow
      .map {
        case (parsed, idx, size) => {
          val envelope = Envelope(dataType = dataType, recordNum = idx, message = parsed)

          val stat = UploadStat(id = id, goodRecords = 1, size = size, maxRecord = size)

          val json = envelopeEncoder(envelope).noSpaces.getBytes()
          val rec = new ProducerRecord(ctx.topic, id, json)

          ProducerMessage.Message(rec, stat)
        }
      }
  }

  override def makeGraph(source: Source[ByteString, _],
                         dataType: String, /* TODO */
                         id: UUID) = {
    def combiner(lf: Future[Long], rf: Future[UploadStat]): Future[UploadStat] = {
      for {
        l <- lf
        r <- rf
      } yield {
        r.copy(badRecords = l)
      }
    }

    source
      .viaMat(messageCreator(id, dataType))(Keep.right)
      .viaMat(producerFlow)(Keep.left)
      .toMat(uploadStatSink(id))(combiner)
  }

}
