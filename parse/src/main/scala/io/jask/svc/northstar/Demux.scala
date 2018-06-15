package io.jask.svc.northstar

import java.util.UUID

import com.typesafe.config.Config
import org.apache.kafka.clients.consumer.KafkaConsumer
import org.slf4j.LoggerFactory
import java.io.{File, FileOutputStream}

import scala.collection.JavaConverters._

import cats.syntax.either._
import io.circe._, io.circe.parser._

class Demux(consumer: KafkaConsumer[UUID, Array[Byte]], config: Config) extends Runnable {

  private[this] lazy val log = LoggerFactory.getLogger(classOf[Demux])

  /** Build and return a map of named outputs for parsing demultiplexed
    * raw records from Kafka.
    *
    * @return Map of named pipes
    */
  def buildOutputsMap(): Map[String, FileOutputStream] = {
    var outputsMap:Map[String, FileOutputStream] = Map()
    val d = new File(config.getString("northstar.parser.folder"))
    if (d.exists && d.isDirectory) {
      for(file <- d.listFiles.toList) {
        try {
         outputsMap += (file.getName -> new FileOutputStream(file))
         log.info("Adding demux output: " + file.getName)
        } catch {
          case e: Throwable => {
            log.error("Skipping output: " + file.getName + " " + e.getMessage)
          }
        }
      }
      outputsMap
    } else {
      Map[String, FileOutputStream]()
    }
  }

  /** Execute a Kafka consumer to forever read and demultiplex records keyed by
    * tag to its corresponding named output.
    */
  def run(): Unit = {
    val topic = config.getString("northstar.consume.topic")
    val timeout = config.getInt("northstar.consume.timeout")
    val outputs = buildOutputsMap()
    log.info("Worker started on topic: ".concat(topic))
    consumer.subscribe(Seq(topic).asJava)

    while(true){
      val records = consumer.poll(timeout).asScala
      for (record <- records) {
        val rawJson = new String(record.value, "UTF-8")
        parse(rawJson) match {
          case Left(failure) => log.error("Skipping non-JSON record: " + rawJson)
          case Right(json) =>  {
            val cursor: HCursor = json.hcursor
            cursor.downField("msg").downField("tag").as[String] match {
              case Left(failure) => log.error("Skipping non-JASK JSON record: " + rawJson)
              case Right(tag) => {
                log.info("Tag is: " + tag)
                if (outputs.contains(tag)) {
                  val output = outputs(tag)
                  output.write(record.value)
                  output.write('\n')
                } else {
                  log.error("No output defined for tag: " + tag)
                }
              }
            }
          }
        }
      }
    }
  }
}
