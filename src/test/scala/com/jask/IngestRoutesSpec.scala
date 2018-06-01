package com.jask

//#ingest-routes-spec
//#test-top
import akka.actor.ActorRef
import akka.http.scaladsl.marshalling.Marshal
import akka.http.scaladsl.model._
import akka.http.scaladsl.testkit.ScalatestRouteTest
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.{ Matchers, WordSpec }

//#set-up
class IngestRoutesSpec extends WordSpec with Matchers with ScalaFutures with ScalatestRouteTest
    with IngestRoutes {
  //#test-top

  // Here we need to implement all the abstract members of IngestRoutes.
  // We use the real IngestActor to test it while we hit the Routes, 
  // but we could "mock" it by implementing it in-place or by using a TestProbe() 
  override val ingestActor: ActorRef =
    system.actorOf(IngestActor.props, "ingest")

  lazy val routes = ingestRoutes

  //#set-up

  //#actual-test
  "IngestRoutes" should {
    "return no logs if no present (GET /logs)" in {
      // note that there's no need for the host part in the uri:
      val request = HttpRequest(uri = "/logs")

      request ~> routes ~> check {
        status should ===(StatusCodes.OK)

        // we expect the response to be json:
        contentType should ===(ContentTypes.`application/json`)

        // and no entries should be in the list:
        entityAs[String] should ===("""""")
      }
    }
    //#actual-test

    //#testing-post
    "be able to add logs (POST /logs)" in {
      val log = "Kapi 42 jp"
      val logEntity = Marshal(log).to[MessageEntity].futureValue // futureValue is from ScalaFutures

      // using the RequestBuilding DSL:
      val request = Post("/logs").withEntity(logEntity)

      request ~> routes ~> check {
        status should ===(StatusCodes.Created)

        // we expect the response to be json:
        contentType should ===(ContentTypes.`application/json`)

        // and we know what message we're expecting back:
        entityAs[String] should ===("""Kapi 42 jp""")
      }
    }
    //#actual-test
  }
  //#actual-test

  //#set-up
}
//#set-up
//#ingest-routes-spec
