package io.jask.svc.northstar

import java.util.UUID

import com.lightbend.kafka.scala.streams.Deserializer

class UUIDBinaryDeserializer extends Deserializer[UUID]
                             with UUIDBinaryDeserializerOps {}
