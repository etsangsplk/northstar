package io.jask.svc.northstar

import java.util.UUID

import akka.stream.scaladsl.{RunnableGraph, Source}
import akka.util.ByteString

import scala.concurrent.Future

/** Connect a byte source to a graph and materialize a response.
  *
  * northstar-http's core behavior does the following:
  *   - Take source bytes sent from the user
  *   - Interpret these bytes as a series of events
  *   - Send those events to persistent storage systems
  *   - Summarize the events as a response for the user.
  *
  * In akka-http terms: we connect each bytestring source to a runnable graph which processes and
  * persists the records from the stream, and materializes a summary value. Each of these graphs
  * that we make use of is constructed by a GraphBuilder[T], where T is the materialized value
  * type.
  *
  * @tparam T Response type
  */
trait GraphBuilder[T] {

  /** Make a graph that handles source bytes
    *
    * @param source   Source bytes from the sensor.
    * @param dataType The "type" of data the sensor is sending.
    * @param id       This particular upload's UUID. (Used for retry deduping.)
    * @return A graph that you can use to handle incoming data, and which materializes a return
    *         value for the requestor.
    */
  def makeGraph(source: Source[ByteString, _],
                dataType: String, /* TODO */
                id: UUID): RunnableGraph[Future[T]]
}
