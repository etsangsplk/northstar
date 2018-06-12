package io.jask.svc.northstar

import java.util.UUID

import akka.actor.ActorSystem
import akka.event.Logging
import akka.http.scaladsl.Http
import akka.http.scaladsl.model.StatusCodes
import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.server.Route
import akka.http.scaladsl.server.directives.RouteDirectives.complete
import akka.kafka.scaladsl.Producer
import akka.kafka.{ProducerMessage, ProducerSettings}
import akka.stream.ActorMaterializer
import akka.stream.scaladsl.Framing
import akka.util.ByteString
import io.jask.dto.northstar.{JsonSupport, UploadStat}
import org.apache.kafka.clients.producer.{KafkaProducer, ProducerRecord}

import scala.util.{Failure, Success}

class Ingest(val topic: String,
             val producer: KafkaProducer[UUID, Array[Byte]],
             val producerSettings: ProducerSettings[UUID, Array[Byte]])
            (implicit val system: ActorSystem,
             implicit val materializer: ActorMaterializer) extends JsonSupport {

  private[this] lazy val log = Logging(system, classOf[Ingest])

  private[this] def pipeline(dataType: String, idOption: Option[UUID] = None) =
    extractDataBytes { data =>
      var count = 0
      val pipe = data
        .via(Framing.delimiter(ByteString("\n"),
                               maximumFrameLength = 100000,
                               allowTruncation = true))
        .map { line: ByteString =>
          val id = idOption.getOrElse(UUID.randomUUID())
          val stat = UploadStat(count = 1, size = line.size, maxRecord = line.size)
          val rec = new ProducerRecord(topic, id, line.toArray)

          ProducerMessage.Message(rec, stat)
        }
        .via(Producer.flow(producerSettings, producer))
        .runFold(UploadStat()) { (acc, message) =>
          val stat = message.message.passThrough

          acc.copy(count = acc.count + 1,
                   size = acc.size + stat.size,
                   failures = acc.failures + stat.failures,
                   maxRecord = math.max(acc.maxRecord, stat.maxRecord))
        }

      onComplete(pipe) {
        case Success(result) => {
          complete(result)
        }
        case Failure(e)      => {
          complete(StatusCodes.InternalServerError, e)
        }
      }
    }

  lazy val route: Route =
    (post & path("log")) {
      pipeline("log")
    } ~
    (post & path("log" / "cef")) {
      pipeline("log")
    } ~
    path("file") {
      complete {
        "hi!"  // TODO!
      }
    } ~
    path("bro" / RemainingPath) { broType =>
      pipeline(broType.toString())
    }

  def run(): Unit = {
    Http().bindAndHandle(route, "0.0.0.0", 8080)
  }
}
