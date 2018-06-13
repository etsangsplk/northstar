package io.jask.svc.northstar

import java.util.{Properties, UUID}

import com.typesafe.config.{Config, ConfigFactory}
import org.apache.kafka.clients.consumer.{ConsumerConfig, KafkaConsumer}

object ParseService {
  def main(args: Array[String]) {
    var config: Config = null
    var consumer: KafkaConsumer[UUID, Array[Byte]] = null

    try {
      config = ConfigFactory.load()
      /**
        * Convert Typesafe config to Java `Properties`.
        */
      val props = new Properties()
      props.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG,
        config.getString("kafka-consumer.bootstrap.servers"))
      props.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG,
        "io.jask.svc.northstar.UUIDDeserializer")
      props.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG,
        "org.apache.kafka.common.serialization.ByteArrayDeserializer")
      props.put(ConsumerConfig.GROUP_ID_CONFIG, config.getString("kafka-consumer.group.id"))
      consumer = new KafkaConsumer[UUID, Array[Byte]](props)

      new Demux(consumer, config).run()
    } catch {
      case e: Throwable => {
        println("Shutting down...")
        e.printStackTrace()
      }
    }
  }
}
