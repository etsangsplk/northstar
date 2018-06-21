package io.jask.svc.northstar

import io.jask.dto.northstar.UploadStat
import org.slf4j.LoggerFactory

import scala.collection.JavaConverters._

/** Tests that the graphs that RecordGraphBuilder constructs do the right thing
  *
  * They should:
  *  - Split inputs on newlines
  *  - Validate that each is JSON
  *  - Send each valid line off to Kafka
  *  - Report on how many good and bad lines total.
  */
class RecordGraphTest extends StreamUnitSpec {
  val log = LoggerFactory.getLogger(this.getClass)

  def fixture(input: String) = {
    new StreamTestFixture(input)
  }

  "The record recordGraph" should "work on an empty input" in {
    val f = fixture("")

    f.recordGraph.run().map { uploadStat =>
      f.mockProducer.history().asScala shouldBe empty

      uploadStat should equal(
        UploadStat(id = f.id, goodRecords = 0, badRecords = 0, size = 0)
      )
    }
  }

  it should "work with one json record" in {
    val input  = """{"howdy": "yall"}"""
    val output = s"""{"dataType":"test","recordNum":0,"message":{"howdy":"yall"}}"""
    val f      = fixture(input)

    f.recordGraph.run().map { uploadStat =>
      f.kafkaHistory should contain((f.id, output))

      uploadStat should equal(
        UploadStat(id = f.id,
                   goodRecords = 1,
                   badRecords = 0,
                   size = input.size,
                   maxRecord = input.size)
      )
    }
  }

  it should "should treat blank lines as bad, but skip them in the record num count" in {
    val goodInput = """{"howdy": "yall"}"""
    val output    = s"""{"dataType":"test","recordNum":1,"message":{"howdy":"yall"}}"""
    val input =
      s"""
         |$goodInput
         |
        |""".stripMargin
    val f = fixture(input)

    f.recordGraph.run().map { uploadStat =>
      f.kafkaHistory should contain((f.id, output))

      uploadStat should equal(
        UploadStat(id = f.id,
                   goodRecords = 1,
                   badRecords = 2,
                   size = goodInput.size,
                   maxRecord = goodInput.size)
      )
    }
  }

  it should "should treat rando text lines as bad, but skip them in the record num count" in {
    val goodInput = """{"howdy": "yall"}"""
    val output1   = s"""{"dataType":"test","recordNum":1,"message":{"howdy":"yall"}}"""
    val output2   = s"""{"dataType":"test","recordNum":3,"message":{"howdy":"yall"}}"""
    val input =
      s"""WHAT
         |$goodInput
         |   IS
         |$goodInput
         |      UP
         |""".stripMargin
    val f = fixture(input)

    f.recordGraph.run().map { uploadStat =>
      f.kafkaHistory should contain inOrder ((f.id, output1), (f.id, output2))

      uploadStat should equal(
        UploadStat(id = f.id,
                   goodRecords = 2,
                   badRecords = 3,
                   size = goodInput.size * 2,
                   maxRecord = goodInput.size)
      )
    }
  }
}
