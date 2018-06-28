package io.jask.svc.northstar.schema

import java.io.OutputStream

import org.apache.avro.generic.GenericRecord

trait AvroSerializable {
  def writeToAvro(os: OutputStream): Unit
  def dataType: String
  def convertToGenericRecord(): GenericRecord
}