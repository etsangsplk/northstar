package io.jask.svc.northstar

import java.util.UUID

import akka.actor.ActorSystem
import akka.stream.alpakka.s3.scaladsl.MultipartUploadResult
import akka.stream.scaladsl.{Keep, RunnableGraph, Sink, Source}
import akka.util.ByteString
import io.jask.dto.northstar.FileStat

import scala.concurrent.{ExecutionContext, Future}

/** Creates runnable graphs that can be used to handle carved file uploads.
  *
  * This graph stores carved files in s3, and materializes a summary of the file content.
  *
  * @param ctx Application-wide variables
  */
class FileGraphBuilder(ctx: IngestContext) extends GraphBuilder[FileStat] {
  private[this] implicit val system: ActorSystem      = ctx.system
  private[this] implicit val ec    : ExecutionContext = ctx.system.dispatcher

  /** A graph sink that simply adds up the bytes to materialize a total size. */
  private[this] val sizeCounterSink =
    Sink.fold(0)((acc: Int, rec: ByteString) => acc + rec.size)

  /** Creates a graph sink that uploads bytes to s3.
    *
    * @param id Creates a graph sink that uploads to this S3 key.
    * @return A graph sink that takes incoming bytes and uploads them to s3 at the given key.
    */
  private[this] def s3UploadSink(id: UUID) = {
    ctx.s3client.multipartUpload(ctx.bucket, id.toString)
  }

  /** Combines the size sink and s3 sink materialized values
    *
    * The third-party S3 sink doesn't materialize any statistics about the actual file it
    * uploaded, so we broadcast the input stream to both the s3 sink and our custom sink, which
    * (for now) simply count bytes.
    *
    * This method is a combiner function that takes the results of each of those sinks and
    * produces a single FileStat DTO for sending back to our customer.
    *
    * @param uploadResult
    * @param count
    * @return
    */
  private[this] def combiner(uploadResult: Future[MultipartUploadResult],
                             count: Future[Int]): Future[FileStat] = {
    for {
      c <- count
      ur <- uploadResult
    } yield {
      FileStat(size = Some(c),
               path = Some(s"s3://${ur.bucket}/${ur.key}"))
    }
  }

  override def makeGraph(source: Source[ByteString, _],
                         dataType: String,
                         id: UUID): RunnableGraph[Future[FileStat]] = {
    source
      .alsoToMat(s3UploadSink(id))(Keep.right)
      .toMat(sizeCounterSink)(combiner)
  }
}
