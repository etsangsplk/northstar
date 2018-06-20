package io.jask.svc.northstar

import java.util.UUID

import com.lightbend.kafka.scala.streams.StatelessScalaSerde

/** Bespoke artisanal UUID serialization/deserialization
  *
  * This class arranges the constituent bytes of a UUID from most to least significant.
  */
class UUIDBinarySerde
    extends StatelessScalaSerde[UUID]
    with UUIDBinarySerializerOps
    with UUIDBinaryDeserializerOps {}
