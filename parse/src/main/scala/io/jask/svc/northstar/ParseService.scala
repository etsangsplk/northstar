package io.jask.svc.northstar

import java.util.{Properties, UUID}

import com.typesafe.config.{Config, ConfigFactory}
import org.apache.kafka.clients.consumer.{ConsumerConfig, KafkaConsumer}
import org.slf4j.LoggerFactory

object ParseService {
  private[this] lazy val log = LoggerFactory.getLogger("ParseService")

  /** Northstar-parse service implements the following core functionality:
    *   - Consume raw data from Kafka and demultiplex to Syslog-NG via tags
    *   - Consume mapped data from Syslog-NG, perform fix-ups, convert to Avro
    *   - Publish final Avro to Kafka on a new topic
    */
  def main(args: Array[String]) {
    var config: Config                             = null
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
                "io.jask.svc.northstar.UUIDBinaryDeserializer")
      props.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG,
                "org.apache.kafka.common.serialization.ByteArrayDeserializer")
      props.put(ConsumerConfig.GROUP_ID_CONFIG, config.getString("kafka-consumer.group.id"))
      props.put(ConsumerConfig.ENABLE_AUTO_COMMIT_CONFIG,
                config.getString("kafka-consumer.enable.auto.commit"))
      props.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG,
                config.getString("kafka-consumer.auto.offset.reset"))
      props.put(ConsumerConfig.MAX_PARTITION_FETCH_BYTES_CONFIG,
                config.getString("kafka-consumer.max.partition.fetch.bytes"))
      props.put(ConsumerConfig.SESSION_TIMEOUT_MS_CONFIG,
                config.getString("kafka-consumer.session.timeout.ms"))
      consumer = new KafkaConsumer[UUID, Array[Byte]](props)

      new Thread(new RawDemuxer(consumer, config)).start()
      new Thread(new RecordPublisher(config)).start()

    } catch {
      case e: Throwable => {
        log.error("Shutting down...", e)
      }
    }
  }
}
