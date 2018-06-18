package io.jask.svc.northstar

import java.util.UUID

import akka.actor.ActorSystem
import akka.stream.Materializer
import akka.stream.alpakka.s3.scaladsl.S3Client
import akka.stream.scaladsl.Source
import akka.util.ByteString
import org.apache.kafka.clients.producer.MockProducer
import org.apache.kafka.common.serialization.ByteArraySerializer

import collection.JavaConverters._

/** Holds all the stream-related stuff we need to construct and use for each test case. */
class StreamTestFixture(input: String)(implicit outerSystem: ActorSystem,
                                       outerMaterializer: Materializer) {
  val outer  = this
  val source = Source.single[ByteString](ByteString(input.getBytes("UTF-8")))
  val id     = UUID.randomUUID()

  val mockProducer =
    new MockProducer(true, new UUIDBinarySerializer(), new ByteArraySerializer())

  /** I don't feel like figuring out how to write a custom matcher that works for matching
    * producer records based on byte-array *contents*, so instead I'll just create this
    * simplified projection of the message history for use in test case matchers.
    */
  def kafkaHistory = mockProducer.history().asScala.map { message =>
    (message.key(), new String(message.value(), "UTF-8"))
  }

  val server = S3TestServer.server

  def fileContent = {
    scala.io.Source
      .fromInputStream(
        server.client.getObject("test-bucket", id.toString()).getObjectContent
      )
      .mkString
  }

  val ctx = new IngestContext {
    override val system           = outerSystem
    override val materializer     = outerMaterializer
    override val topic            = "test-topic"
    override val bucket           = "test-bucket"
    override val producer         = mockProducer
    override val producerSettings = IngestService.producerSettings(system)
    override val s3client         = S3Client()
  }

  val recordGraphBuilder = new RecordGraphBuilder(ctx)
  val fileGraphBuilder   = new FileGraphBuilder(ctx)

  val fileGraph   = fileGraphBuilder.makeGraph(source, "test", id)
  val recordGraph = recordGraphBuilder.makeGraph(source, "test", id)
}
