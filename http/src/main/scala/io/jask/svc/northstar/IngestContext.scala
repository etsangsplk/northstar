package io.jask.svc.northstar

import java.util.UUID

import akka.actor.ActorSystem
import akka.kafka.ProducerSettings
import akka.stream.ActorMaterializer
import akka.stream.alpakka.s3.scaladsl.S3Client
import org.apache.kafka.clients.producer.KafkaProducer

trait IngestContext {
  val system: ActorSystem
  val materializer: ActorMaterializer

  val topic: String
  val bucket: String
  val producer: KafkaProducer[UUID, Array[Byte]]
  val producerSettings: ProducerSettings[UUID, Array[Byte]]
  val s3client: S3Client
}
