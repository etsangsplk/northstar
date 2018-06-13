package io.jask.svc.northstar

import java.util.UUID

trait UUIDBinarySerializerOps {
  def serialize(data: UUID): Array[Byte] = {
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
}
