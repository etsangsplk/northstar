package io.jask.svc.northstar

import java.util.UUID

import akka.actor.ActorSystem
import akka.event.Logging
import akka.http.scaladsl.Http
import akka.http.scaladsl.model.{Multipart, StatusCodes}
import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.server.Route
import akka.http.scaladsl.server.directives.MarshallingDirectives.{as, entity}
import akka.http.scaladsl.server.directives.RouteDirectives.complete
import akka.kafka.scaladsl.Producer
import akka.kafka.{ProducerMessage, ProducerSettings}
import akka.stream.alpakka.s3.scaladsl.{MultipartUploadResult, S3Client}
import akka.stream.scaladsl.{Broadcast, Framing, GraphDSL, RunnableGraph, Sink, Source}
import akka.stream.{ActorMaterializer, ClosedShape}
import akka.util.ByteString
import io.jask.dto.northstar.{FileStat, FileUploadStat, JsonSupport, UploadStat}
import org.apache.kafka.clients.producer.{KafkaProducer, ProducerRecord}

import scala.concurrent.{ExecutionContext, Future}
import scala.util.{Failure, Success}

class Ingest(topic: String,
             bucket: String,
             producer: KafkaProducer[UUID, Array[Byte]],
             producerSettings: ProducerSettings[UUID, Array[Byte]],
             s3client: S3Client)
            (implicit val system: ActorSystem,
             implicit val materializer: ActorMaterializer) extends JsonSupport {

  private[this] implicit val ec: ExecutionContext = system.dispatcher
  private[this] lazy     val log                  = Logging(system, classOf[Ingest])

  private[this] def kafkaPipeline(data: Source[ByteString, Any],
                                  dataType: String,
                                  idOption: Option[UUID] = None): Future[UploadStat] = {
    data.
      via(Framing.delimiter(ByteString("\n"),
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
  }

  private[this] def s3pipeline(data: Source[ByteString, Any],
                               dataType: String,
                               id: UUID): Future[FileStat] = {

    val upload = s3client.multipartUpload(bucket, id.toString)
    val count = Sink.fold(0)((acc: Int, rec: ByteString) => acc + rec.size)
    val combiner = (count: Future[Int], uploadResult: Future[MultipartUploadResult]) => {
      for {
        c <- count
        ur <- uploadResult
      } yield {
        FileStat(size = Some(c),
                 path = Some(s"s3://${ur.bucket}/${ur.key}"))
      }
    }

    val graph = GraphDSL.create(count, upload)(combiner) { implicit builder =>
      (count, upload) => {
        import GraphDSL.Implicits._

        val bcast = builder.add(Broadcast[ByteString](2))

        data ~> bcast ~> count
        bcast ~> upload

        ClosedShape
      }
    }

    RunnableGraph.fromGraph(graph).run
  }

  private[this] def recordRoute(dataType: String,
                                idOption: Option[UUID] = None): Route = {
    extractDataBytes { data =>
      val pipe = kafkaPipeline(data, dataType, idOption)

      onComplete(pipe) {
        case Success(result) => {
          complete(result)
        }
        case Failure(e)      => {
          failWith(e)
        }
      }
    }
  }

  private[this] def fileRoute(id: UUID): Route = {
    entity(as[Multipart.FormData]) { formData =>
      val pipe: Future[FileUploadStat] = formData.parts
        .runFoldAsync(FileUploadStat()) { (stat, part) =>
          part.name match {
            case "record"        => {
              kafkaPipeline(part.entity.dataBytes, "file", Some(id)).map { uploadStat =>
                stat.copy(uploadStat = Some(uploadStat))
              }
            }
            case "extractedFile" => {
              s3pipeline(part.entity.dataBytes, "file", id).map { fileStat =>
                stat.copy(fileStat = Some(fileStat))
              }
            }
            case _               =>
              part.entity.dataBytes.runFold(stat)((_, _) => stat)
          }
        }

      onComplete(pipe) {
        case Success(u) => {
          u match {
            case FileUploadStat(Some(_), Some(_)) =>
              complete(u)
            case _ =>
              complete((StatusCodes.BadRequest,
                         "Must send parts named 'record' and 'extractedFile'"))
          }
        }
        case Failure(e) => {
          failWith(e)
        }
      }
    }
  }

  lazy val route: Route =
    (post & (path("log") | path("log" / "cef"))) {
      recordRoute("log")
    } ~
    (post & (path("user") | path("inventory"))) {
      recordRoute("entity")
    } ~
    (post & path("bro" / RemainingPath)) { broType =>
      recordRoute(s"network.${broType.toString()}")
    } ~
    (post & path("file")) {
      fileRoute(UUID.randomUUID())
    }

  def run(): Unit = {
    Http().bindAndHandle(route, "0.0.0.0", 8080)
  }
}
