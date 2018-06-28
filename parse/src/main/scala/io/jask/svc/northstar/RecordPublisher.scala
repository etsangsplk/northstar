package io.jask.svc.northstar

import java.util.Properties
import java.io.{BufferedReader, File, FileInputStream, InputStreamReader}

import com.typesafe.config.Config
import io.circe.Json
import org.slf4j.LoggerFactory
import org.apache.kafka.clients.producer.{KafkaProducer, ProducerConfig, ProducerRecord}
import io.circe.jawn._
import io.jask.svc.northstar.schema.{AvroSerializable, BlueCoatRecord}
import org.apache.avro.generic.GenericRecord

class RecordPublisher(config: Config) extends Runnable {
  private[this] lazy val log = LoggerFactory.getLogger(classOf[RecordPublisher])

  /** Attempt to open an input pipe from Syslog-NG.
    *
    * @param pipe
    * @return FileInputStream on success (null on failure)
    */
  def buildInputStream(pipe: String): FileInputStream = {
    try {
      new FileInputStream(new File(pipe))
    }
    catch {
      case e: Throwable => {
        {
          log.error("Error opening input pipe: ".concat(pipe), e)
          Thread.sleep(1000)
        }
        null
      }
    }
  }

  /** Execute a Kafka consumer to forever read and demultiplex records keyed by
    * tag to its corresponding named output.
    */
  def run(): Unit = {
    val props = new Properties()
    val topic = config.getString("northstar.produce.topic")
    val pipe = config.getString("northstar.parse.output.pipe")
    var input: FileInputStream = null
   props.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG,
              config.getString("kafka-consumer.bootstrap.servers"))
    props.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG,
             "io.confluent.kafka.serializers.KafkaAvroSerializer")
    props.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG,
            "io.confluent.kafka.serializers.KafkaAvroSerializer")
//    props.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG,
//              "org.apache.kafka.common.serialization.StringSerializer")
//    props.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG,
//              "org.apache.kafka.common.serialization.StringSerializer")
//    val producer = new KafkaProducer[Null, GenericRecord](props)
    log.info("Record publisher started on topic: ".concat(topic))
    while (true) {
      if (input == null) {
        input = buildInputStream(pipe)
        log.info("Waiting for input...")
      }
      else {
        log.info("Hello input!!!")
        val bufferedRead = new BufferedReader(new InputStreamReader(input))
        bufferedRead.lines().forEach {
          line => {
            parseJson(line)
//           producer.send(new ProducerRecord[Null, GenericRecord]("test", null, parseJson(line)
//              .convertToGenericRecord()))
          }
        }
        Thread.sleep(1000)

      }
    }
  }

  def parseJson(line: String) : AvroSerializable = {
      log.debug("Raw Json: " + line ) //TODO debugging
      decode[BlueCoatRecord](line) match {
        case Left(error) => {
          log.warn("Cannot parse json: " + line)
          error.fillInStackTrace()
          null //TODO Is there a more correct way to do this
        }
        case Right(record) => {
          log.debug("Parsed Json: " + record.toString()) //TODO debugging
          record
        }
      }
  }
}
