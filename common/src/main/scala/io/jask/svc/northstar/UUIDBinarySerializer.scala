package io.jask.svc.northstar

import java.util.UUID

import com.lightbend.kafka.scala.streams.Serializer

class UUIDBinarySerializer extends Serializer[UUID] with UUIDBinarySerializerOps {}
