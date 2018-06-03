package com.jask

import scala.concurrent.Await
import scala.concurrent.duration.Duration
import akka.actor.ActorSystem
import akka.kafka.ProducerSettings
import akka.stream.ActorMaterializer
import com.typesafe.config.{Config, ConfigFactory}
import org.apache.kafka.clients.producer.KafkaProducer
import org.apache.kafka.common.serialization.{ByteArraySerializer,
  StringSerializer}

object IngestService {
  def main(args: Array[String]) {
    var config: Config = null
    implicit var system: ActorSystem = null
    implicit var materializer: ActorMaterializer = null
    var producer: KafkaProducer[String, Array[Byte]] = null
    var producerSettings: ProducerSettings[String, Array[Byte]] = null

    try {
      config = ConfigFactory.load()
      system = ActorSystem("Northstar", config)
      materializer = ActorMaterializer()

      producerSettings = ProducerSettings(system,
                                          new StringSerializer(),
                                          new ByteArraySerializer())

      producer = producerSettings.createKafkaProducer()

      val topic = config.getString("northstar.produce.topic")

      new Ingest(topic, producer, producerSettings).run()
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
