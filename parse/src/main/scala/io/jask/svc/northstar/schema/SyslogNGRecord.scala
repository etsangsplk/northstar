package io.jask.svc.northstar.schema

import java.io.OutputStream

import com.sksamuel.avro4s.{AvroOutputStream, RecordFormat}
import io.circe.Decoder
import org.apache.avro.generic.GenericRecord
import io.circe.generic.semiauto.deriveDecoder

case class Destination(host: Option[String], port: Option[Int], ip: Option[String])

case class Event(date: Option[String], time: Option[String], duration: Option[Long],
                   virusId: Option[Long], exceptionId: Option[String])

case class Http(result: Option[String], category: Option[String], referer: Option[String],
                 status: Option[String], cacheAction: Option[String], method: Option[String],
                 contentType: Option[String], scheme: Option[String], path: Option[String],
                 query: Option[String], extension: Option[String], bytes: Option[Long])

case class Source(ip: Option[String], username: Option[String], group: Option[String],
                  userAgent: Option[String])


case class SyslogNGRecord(event: Option[Event],
                          destination: Option[Destination],
                          source: Option[Source],
                          http: Option[Http],
                          TAGS: Option[String],
                          SOURCEIP: Option[String],
                          SOURCE: Option[String],
                          PRIORITY: Option[String],
                          MESSAGE: Option[String],
                          HOST_FROM: Option[String],
                          HOST: Option[String],
                          FILE_NAME: Option[String],
                          FACILITY: Option[String],
                          DATE: Option[String]
                         ) extends AvroSerializable {

  override def dataType: String = {
    "logRecord"
  }

  override def writeToAvro(os: OutputStream) = {
    def avroStream = {
      AvroOutputStream.data[SyslogNGRecord](os)
    }
    avroStream.write(this)
    avroStream.flush()
    avroStream.close()
  }

  override def convertToGenericRecord(): GenericRecord = {
    RecordFormat[SyslogNGRecord].to(this)
  }
}

object SyslogNGRecord {
  implicit val decode: Decoder[SyslogNGRecord] = deriveDecoder[SyslogNGRecord]
  implicit val decodeDestination: Decoder[Destination] = deriveDecoder[Destination]
  implicit val decodeEvent: Decoder[Event] = deriveDecoder[Event]
  implicit val decodeHttp: Decoder[Http] = deriveDecoder[Http]
  implicit val decodeSource: Decoder[Source] = deriveDecoder[Source]
}

