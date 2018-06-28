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


case class BlueCoatRecord(event: Option[Event],
                           destination: Option[Destination],
                           source: Option[Source],
                           http: Option[Http]
                         ) extends AvroSerializable { //TODO add fields from top-level json

  override def dataType: String = {
    "blueCoatRecord"
  }

  override def writeToAvro(os: OutputStream) = {
    def avroStream = {
      AvroOutputStream.data[BlueCoatRecord](os)
    }
    avroStream.write(this)
    avroStream.flush()
    avroStream.close()
  }

  override def convertToGenericRecord(): GenericRecord = {
    RecordFormat[BlueCoatRecord].to(this)
  }
}

object BlueCoatRecord {
  implicit val decode: Decoder[BlueCoatRecord] = deriveDecoder[BlueCoatRecord]
  implicit val decodeDestination: Decoder[Destination] = deriveDecoder[Destination]
  implicit val decodeEvent: Decoder[Event] = deriveDecoder[Event]
  implicit val decodeHttp: Decoder[Http] = deriveDecoder[Http]
  implicit val decodeSource: Decoder[Source] = deriveDecoder[Source]
}

