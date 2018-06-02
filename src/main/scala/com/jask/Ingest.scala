package com.jask

import akka.actor.ActorSystem
import akka.event.Logging
import akka.http.scaladsl.Http
import akka.http.scaladsl.marshallers.sprayjson.SprayJsonSupport
import akka.http.scaladsl.model.StatusCodes
import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.server.Route
import akka.http.scaladsl.server.directives.RouteDirectives.complete
import akka.kafka.{ProducerMessage, ProducerSettings}
import akka.kafka.scaladsl.Producer
import akka.stream.ActorMaterializer
import akka.stream.scaladsl.Framing
import akka.util.ByteString
import org.apache.kafka.clients.producer.{KafkaProducer, ProducerRecord}
import spray.json.DefaultJsonProtocol

import scala.util.{Failure, Success}

case class UploadStat(count: Long = 0,
                      size: Long = 0,
                      maxRecord: Long = Long.MinValue)

trait JsonSupport extends SprayJsonSupport with DefaultJsonProtocol {
  implicit val itemFormat = jsonFormat3(UploadStat)
}

class Ingest(val topic: String,
             val producer: KafkaProducer[String, Array[Byte]],
             val producerSettings: ProducerSettings[String, Array[Byte]])
            (implicit val system: ActorSystem,
             implicit val materializer: ActorMaterializer) extends JsonSupport {

  lazy val log = Logging(system, classOf[Ingest])

  lazy val logRoute: Route =
    extractDataBytes { data =>
      val pipe = data
        .via(Framing.delimiter(ByteString("\n"),
                               maximumFrameLength = 100000,
                               allowTruncation = true))
        .map { line =>
          val rec = new ProducerRecord[String, Array[Byte]](topic, line.toArray)
          ProducerMessage.Message(rec, line)
        }
        .via(Producer.flow(producerSettings, producer))
        .runFold(UploadStat()) { (acc, thing) =>
          val line = thing.message.passThrough

          acc.copy(count = acc.count + 1,
                   size = acc.size + line.size,
                   maxRecord = math.max(acc.maxRecord, line.size))
        }

      onComplete(pipe) {
        case Success(result) => {
          complete(result)
        }
        case Failure(e)      => {
          complete(StatusCodes.InternalServerError)
        }
      }
    }

  lazy val route: Route =
    post {
      path("log") {
        logRoute
      } ~
      path("log" / "cef") {
        logRoute
      } ~
      path("file") {
        complete {
          "hi!"
        }
      } ~
      path("bro" / RemainingPath) { broType =>
        complete {
          s"hi, ${broType}"
        }
      }
    }

  def run(): Unit = {
    Http().bindAndHandle(route, "0.0.0.0", 8080)
  }
}
