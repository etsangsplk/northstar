package io.jask.svc.northstar

import java.util.UUID

import akka.Done
import akka.actor.ActorSystem
import akka.event.Logging
import akka.http.scaladsl.Http
import akka.http.scaladsl.model.{Multipart, StatusCodes}
import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.server.Route
import akka.http.scaladsl.server.directives.MarshallingDirectives.{as, entity}
import akka.http.scaladsl.server.directives.RouteDirectives.complete
import akka.stream.Materializer
import akka.stream.scaladsl.{Keep, Sink}
import io.jask.dto.northstar.{FileUploadStat, JsonSupport}

import scala.concurrent.{ExecutionContext, Future}
import scala.util.{Failure, Success}

class IngestRoutes(ctx: IngestContext,
                   recordGraphBuilder: RecordGraphBuilder,
                   fileGraphBuilder: FileGraphBuilder)
    extends JsonSupport {
  private[this] implicit val system: ActorSystem  = ctx.system
  private[this] implicit val ec: ExecutionContext = ctx.system.dispatcher
  private[this] implicit val mat: Materializer    = ctx.materializer
  private[this] lazy val log                      = Logging(ctx.system, classOf[IngestRoutes])

  private[this] def recordRoute(dataType: String, id: UUID): Route = {
    extractDataBytes { data =>
      val graph = recordGraphBuilder.makeGraph(data, dataType, id).run()

      onComplete(graph) {
        case Success(result) => {
          complete(result)
        }
        case Failure(e) => {
          failWith(e)
        }
      }
    }
  }

  private[this] def fileRoute(id: UUID): Route = {
    entity(as[Multipart.FormData]) { formData =>
      val graph: Future[FileUploadStat] = formData.parts
        .runFoldAsync(FileUploadStat()) { (stat, part) =>
          part.name match {
            case "record" => {
              val flow =
                recordGraphBuilder.makeGraph(part.entity.dataBytes, "file", id)

              flow.run().map { uploadStat =>
                stat.copy(uploadStat = Some(uploadStat))
              }
            }
            case "extractedFile" => {
              val flow = fileGraphBuilder
                .makeGraph(part.entity.dataBytes, "file", id)
                .run()

              flow.map { fileStat =>
                stat.copy(fileStat = Some(fileStat))
              }
            }
            case _ => {
              part.entity.dataBytes.runFold(stat)((_, _) => stat)
            }
          }
        }

      onComplete(graph) {
        case Success(u) => {
          u match {
            case FileUploadStat(Some(_), Some(_)) => {
              complete(u)
            }
            case _ => {
              complete(
                (StatusCodes.BadRequest, "Must send parts named 'record' and 'extractedFile'"))
            }
          }
        }
        case Failure(e) => {
          failWith(e)
        }
      }
    }
  }

  /** Suck all the request body data off so that we can return a 400
    *
    * If you don't actually consume all the request body data, Akka will force-close the
    * connection and give no useful feedback to the user.
    */
  private[this] val defaultRoute: Route = {
    extractDataBytes { data =>
      val pipe: Future[Done] = data.toMat(Sink.ignore)(Keep.right).run()

      onComplete(pipe) {
        case _ =>
          complete((StatusCodes.BadRequest, "The requested resource could not be found"))
      }
    }
  }

  lazy val route: Route =
    post {
      val id = UUID.randomUUID()

      (path("log") | path("log" / "cef")) {
        recordRoute("log", id)
      } ~
        (path("user") | path("inventory")) {
          recordRoute("entity", id)
        } ~
        path("bro" / Remaining) { broType =>
          recordRoute(s"network.${broType}", id)
        } ~
        path("file") {
          fileRoute(id)
        }
    } ~
      put {
        (path("log" / JavaUUID) | path("log" / "cef" / JavaUUID)) { id =>
          recordRoute("log", id)
        } ~
          (path("user" / JavaUUID) | path("inventory" / JavaUUID)) { id =>
            recordRoute("entity", id)
          } ~
          path("bro" / Segment / JavaUUID) { (broType, id) =>
            recordRoute(s"network.${broType}", id)
          } ~
          path("file" / JavaUUID) { id =>
            fileRoute(id)
          }
      } ~ defaultRoute

  def run(): Unit = {
    Http().bindAndHandle(route, "0.0.0.0", 8080)
  }
}
