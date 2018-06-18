package io.jask.svc.northstar

import java.util.UUID

import akka.actor.ActorSystem
import akka.kafka.ProducerSettings
import akka.stream.Materializer
import akka.stream.alpakka.s3.scaladsl.S3Client
import org.apache.kafka.clients.producer.Producer

trait IngestContext {
  val system: ActorSystem
  val materializer: Materializer

  val topic: String
  val bucket: String
  val producer: Producer[UUID, Array[Byte]]
  val producerSettings: ProducerSettings[UUID, Array[Byte]]
  val s3client: S3Client
}
