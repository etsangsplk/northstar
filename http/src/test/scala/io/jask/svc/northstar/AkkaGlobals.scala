package io.jask.svc.northstar

import akka.actor.ActorSystem
import akka.stream.ActorMaterializer
import com.typesafe.config.ConfigFactory

import collection.JavaConverters._

trait AkkaGlobals {
  val config = ConfigFactory.parseMap(
    Map(
      "akka.stream.alpakka.s3.proxy.host"                        -> "localhost",
      "akka.stream.alpakka.s3.proxy.port"                        -> S3TestServer.server.port,
      "akka.stream.alpakka.s3.proxy.secure"                      -> false,
      "akka.stream.alpakka.s3.path-style-access"                 -> true,
      "akka.stream.alpakka.s3.aws.credentials.provider"          -> "static",
      "akka.stream.alpakka.s3.aws.credentials.access-key-id"     -> "foo",
      "akka.stream.alpakka.s3.aws.credentials.secret-access-key" -> "bar",
      "akka.stream.alpakka.s3.aws.region.provider"               -> "static",
      "akka.stream.alpakka.s3.aws.region.default-region"         -> "us-east-1"
    ).asJava)

  implicit val system       = ActorSystem("NorthstarTest", config)
  implicit val materializer = ActorMaterializer()
}
