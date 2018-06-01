package com.jask

//#quick-start-server
import scala.concurrent.Await
import scala.concurrent.duration.Duration

import akka.actor.{ ActorRef, ActorSystem }
import akka.http.scaladsl.Http
import akka.http.scaladsl.server.Route
import akka.stream.ActorMaterializer

//#main-class
object QuickstartServer extends App with IngestRoutes {

  // set up ActorSystem and other dependencies here
  //#main-class
  //#server-bootstrapping
  implicit val system: ActorSystem = ActorSystem("Northstar")
  implicit val materializer: ActorMaterializer = ActorMaterializer()
  //#server-bootstrapping

  val ingestActor: ActorRef = system.actorOf(IngestActor.props, "ingestActor")

  //#main-class
  // from the ingestRoutes trait
  lazy val routes: Route = ingestRoutes
  //#main-class

  //#http-server
  Http().bindAndHandle(routes, "localhost", 8080)

  println(s"Server online at http://localhost:8080/")

  Await.result(system.whenTerminated, Duration.Inf)
  //#http-server
  //#main-class
}
//#main-class
//#quick-start-server
