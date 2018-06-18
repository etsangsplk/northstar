package io.jask.svc.northstar

import java.io.ByteArrayInputStream

import akka.http.scaladsl.server
import org.slf4j.LoggerFactory

/** Tests that the FileGraphBuilder constructs graphs that do the right thing:
  *
  *  - It should upload the file given to s3
  *  - It should count the bytes and report them.
  */
class FileGraphTest extends StreamUnitSpec {
  val log = LoggerFactory.getLogger(this.getClass)

  val outer = this

  def fixture(input: String) = new StreamTestFixture(input)

  "The file fileGraph" should "work on an empty file" in {
    val f = fixture("")

    f.fileGraph.run().map { uploadResult =>
      assert(f.fileContent === "")
      assert(uploadResult.size === Some(0))
    }
  }

  it should "work on a random file" in {
    val input = "hihellohowareya"
    val f     = fixture(input)

    f.fileGraph.run().map { uploadResult =>
      assert(f.fileContent === input)
      assert(uploadResult.size === Some(input.size))
    }
  }
}
