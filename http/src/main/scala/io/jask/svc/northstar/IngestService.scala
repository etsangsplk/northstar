package io.jask.svc.northstar

import java.util.UUID

import akka.actor.ActorSystem
import akka.kafka.ProducerSettings
import akka.stream.ActorMaterializer
import akka.stream.alpakka.s3.scaladsl.S3Client
import com.typesafe.config.{Config, ConfigFactory}
import org.apache.kafka.clients.producer.KafkaProducer
import org.apache.kafka.common.serialization.ByteArraySerializer

import scala.concurrent.Await
import scala.concurrent.duration.Duration

object IngestService {
  def main(args: Array[String]) {
    var config: Config = null
    implicit var system: ActorSystem = null
    implicit var materializer: ActorMaterializer = null
    var producer: KafkaProducer[UUID, Array[Byte]] = null
    var producerSettings: ProducerSettings[UUID, Array[Byte]] = null

    try {
      config = ConfigFactory.load()
      system = ActorSystem("Northstar", config)
      materializer = ActorMaterializer()

      producerSettings = ProducerSettings(system,
                                          new UUIDBinarySerde().serializer(),
                                          new ByteArraySerializer())

      producer = producerSettings.createKafkaProducer()

      val topic = config.getString("northstar.produce.topic")
      val bucket = config.getString("northstar.produce.bucket")
      val s3client = S3Client()

      new Ingest(topic, bucket, producer, producerSettings, s3client).run()
    } catch {
      case e: Throwable => {
        println("Shutting down...")
        e.printStackTrace()

        if (system != null) {
          system.terminate()
        }

        if (materializer != null) {
          materializer.shutdown()
        }
      }
    } finally {
      if (system != null) {
        Await.result(system.whenTerminated, Duration.Inf)
      }
    }
  }
}
