package io.jask.svc.northstar

import akka.actor.ActorSystem
import akka.kafka.ProducerSettings
import akka.stream.ActorMaterializer
import akka.stream.alpakka.s3.scaladsl.S3Client
import com.typesafe.config.ConfigFactory
import org.apache.kafka.common.serialization.ByteArraySerializer

object IngestService {
  def main(args: Array[String]) {
    val config = ConfigFactory.load()

    val context = new IngestContext {
      override val system       = ActorSystem("Northstar", config)
      override val materializer = ActorMaterializer()(system)
      override val topic        = config.getString("northstar.produce.topic")
      override val bucket       = config.getString("northstar.produce.bucket")
      override val producerSettings =
        ProducerSettings(system, new UUIDBinarySerde().serializer(), new ByteArraySerializer())
      override val producer = producerSettings.createKafkaProducer()
      override val s3client = S3Client()(system, materializer)
    }

    val recordGraphBuilder = new RecordGraphBuilder(context)
    val fileGraphBuilder   = new FileGraphBuilder(context)
    val routes             = new IngestRoutes(context, recordGraphBuilder, fileGraphBuilder)

    routes.run()
  }
}
