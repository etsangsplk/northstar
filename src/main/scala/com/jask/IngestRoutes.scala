package com.jask

import akka.actor.{ ActorRef, ActorSystem }
import akka.event.Logging

import scala.concurrent.duration._
import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.model.StatusCodes
import akka.http.scaladsl.server.Route
import akka.http.scaladsl.server.directives.MethodDirectives.delete
import akka.http.scaladsl.server.directives.MethodDirectives.get
import akka.http.scaladsl.server.directives.MethodDirectives.post
import akka.http.scaladsl.server.directives.RouteDirectives.complete
import akka.http.scaladsl.server.directives.PathDirectives.path

import scala.concurrent.Future
import com.jask.IngestActor._
import akka.pattern.ask
import akka.util.Timeout

//#ingest-routes-class
trait IngestRoutes {
  //#ingest-routes-class

  // we leave these abstract, since they will be provided by the App
  implicit def system: ActorSystem

  lazy val log = Logging(system, classOf[IngestRoutes])

  // other dependencies that IngestRoutes use
  def ingestActor: ActorRef

  // Required by the `ask` (?) method below
  implicit lazy val timeout = Timeout(5.seconds) // usually we'd obtain the timeout from the system's configuration

  //#all-routes
  //#logs-post
  lazy val ingestRoutes: Route =
    pathPrefix("logs") {
      concat(
        //#logs-post
        pathEnd {
          concat(
            get {
              val logs: Future[String] =
                (ingestActor ? GetLogs)
              complete(logs)
            },
            post {
              entity(as[User]) { log =>
                val logIngested: Future[ActionPerformed] =
                  (ingestActor ? IngestLog(log)).mapTo[ActionPerformed]
                onSuccess(logIngested) { performed =>
                  log.info("Ingested log [{}]", performed.description)
                  complete((StatusCodes.Created, performed))
                }
              }
            })
        })
      //#logs-post
    }
  //#all-routes
}
