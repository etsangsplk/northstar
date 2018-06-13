package io.jask.svc.northstar

import java.util
import java.util.UUID

import org.apache.kafka.common.errors.SerializationException
import org.apache.kafka.common.serialization.Deserializer

class UUIDDeserializer extends Deserializer[UUID] {
  override def configure(configs: util.Map[String, _], isKey: Boolean): Unit = {}

  override def deserialize(topic: String, data: Array[Byte]): UUID =
    try {
      UUID.fromString(new String(data, "UTF-8"))
    } catch {
      case e: Throwable =>
        throw new SerializationException("Failed to deserialize uuid data: " + e.getMessage)
    }

  override def close(): Unit = {}
}
