package io.jask.svc.northstar

import java.util.{Collections, UUID}

import com.typesafe.config.Config
import org.apache.kafka.clients.consumer.KafkaConsumer
import org.slf4j.LoggerFactory

import scala.collection.JavaConverters._

class Demux(consumer: KafkaConsumer[UUID, Array[Byte]], config: Config) {
  private[this] lazy val log = LoggerFactory.getLogger(classOf[Demux])

  def run(): Unit = {
    val running = true
    val topic = config.getString("northstar.consume.topic")
    val timeout = config.getInt("northstar.consume.timeout")
    log.info("Worker started on topic: ".concat(topic))
    consumer.subscribe(Collections.singletonList(topic))

    while(running){
      val records = consumer.poll(timeout).asScala
      for (record <- records) { log.info(record.toString()) }
    }
  }
}
