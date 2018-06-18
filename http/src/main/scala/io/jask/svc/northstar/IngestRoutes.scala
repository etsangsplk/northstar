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
import akka.stream.Materializer
import io.jask.dto.northstar.{FileUploadStat, JsonSupport}

import scala.concurrent.{ExecutionContext, Future}
import scala.util.{Failure, Success}

class IngestRoutes(ctx: IngestContext,
                   recordGraphBuilder: RecordGraphBuilder,
                   fileGraphBuilder: FileGraphBuilder) extends JsonSupport {
  private[this] implicit val system: ActorSystem      = ctx.system
  private[this] implicit val ec    : ExecutionContext = ctx.system.dispatcher
  private[this] implicit val mat   : Materializer     = ctx.materializer
  private[this] lazy     val log                      = Logging(ctx.system, classOf[IngestRoutes])

  private[this] def recordRoute(dataType: String, id: UUID): Route = {
    extractDataBytes { data =>
      val graph = recordGraphBuilder.makeGraph(data, dataType, id).run()

      onComplete(graph) {
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
      val graph: Future[FileUploadStat] = formData.parts
        .runFoldAsync(FileUploadStat()) { (stat, part) =>
          part.name match {
            case "record"        => {
              val flow = recordGraphBuilder.makeGraph(part.entity.dataBytes, "file", id)

              flow.run().map { uploadStat =>
                stat.copy(uploadStat = Some(uploadStat))
              }
            }
            case "extractedFile" => {
              val flow = fileGraphBuilder.makeGraph(part.entity.dataBytes, "file", id).run()

              flow.map { fileStat =>
                stat.copy(fileStat = Some(fileStat))
              }
            }
            case _               => {
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
            case _                                => {
              complete((StatusCodes.BadRequest,
                        "Must send parts named 'record' and 'extractedFile'"))
            }
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
      recordRoute("log", UUID.randomUUID())
    } ~
    (post & (path("user") | path("inventory"))) {
      recordRoute("entity", UUID.randomUUID())
    } ~
    (post & path("bro" / RemainingPath)) { broType =>
      recordRoute(s"network.${broType.toString()}", UUID.randomUUID())
    } ~
    (post & path("file")) {
      fileRoute(UUID.randomUUID())
    }

  def run(): Unit = {
    Http().bindAndHandle(route, "0.0.0.0", 8080)
  }
}
