package io.jask.svc.northstar

import java.util.UUID

import com.lightbend.kafka.scala.streams.StatelessScalaSerde

/** Bespoke artisanal UUID serialization/deserialization
  *
  * This class arranges the constituent bytes of a UUID from most to least significant.
  */
class UUIDBinarySerde extends StatelessScalaSerde[UUID] {
  override def serialize(data: UUID): Array[Byte] = {
    val msb = data.getMostSignificantBits
    val lsb = data.getLeastSignificantBits

    return Array((msb >>> (8 * 7)).toByte,
                 (msb >>> (8 * 6)).toByte,
                 (msb >>> (8 * 5)).toByte,
                 (msb >>> (8 * 4)).toByte,
                 (msb >>> (8 * 3)).toByte,
                 (msb >>> (8 * 2)).toByte,
                 (msb >>> (8 * 1)).toByte,
                 (msb >>> (8 * 0)).toByte,
                 (lsb >>> (8 * 7)).toByte,
                 (lsb >>> (8 * 6)).toByte,
                 (lsb >>> (8 * 5)).toByte,
                 (lsb >>> (8 * 4)).toByte,
                 (lsb >>> (8 * 3)).toByte,
                 (lsb >>> (8 * 2)).toByte,
                 (lsb >>> (8 * 1)).toByte,
                 (lsb >>> (8 * 0)).toByte)
  }

  override def deserialize(data: Array[Byte]): Option[UUID] = {
    if (data.size == 16) {
      val msb = (data(0).toLong & 0xFF) << (8 * 7) |
                (data(1).toLong & 0xFF) << (8 * 6) |
                (data(2).toLong & 0xFF) << (8 * 5) |
                (data(3).toLong & 0xFF) << (8 * 4) |
                (data(4).toLong & 0xFF) << (8 * 3) |
                (data(5).toLong & 0xFF) << (8 * 2) |
                (data(6).toLong & 0xFF) << (8 * 1) |
                (data(7).toLong & 0xFF) << (8 * 0)
      val lsb = (data(8).toLong & 0xFF) << (8 * 7) |
                (data(9).toLong & 0xFF) << (8 * 6) |
                (data(10).toLong & 0xFF) << (8 * 5) |
                (data(11).toLong & 0xFF) << (8 * 4) |
                (data(12).toLong & 0xFF) << (8 * 3) |
                (data(13).toLong & 0xFF) << (8 * 2) |
                (data(14).toLong & 0xFF) << (8 * 1) |
                (data(15).toLong & 0xFF) << (8 * 0)
      return Some(new UUID(msb, lsb))
    } else {
      return None
    }
  }
}

