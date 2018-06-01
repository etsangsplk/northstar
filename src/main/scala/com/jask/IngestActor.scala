package com.jask

//#ingest-actor
import akka.actor.{ Actor, ActorLogging, Props }

object IngestActor {
  final case class ActionPerformed(description: String)
  final case object GetLogs
  final case class IngestLog(log: String)

  def props: Props = Props[IngestActor]
}

class IngestActor extends Actor with ActorLogging {
  import IngestActor._

  var logs = ""

  def receive: Receive = {
    case GetLogs =>
      sender() ! logs
    case IngestLog(log) =>
      logs = logs.concat(log)
      sender() ! ActionPerformed(s"\${log}")
  }
}
//#ingest-actor
