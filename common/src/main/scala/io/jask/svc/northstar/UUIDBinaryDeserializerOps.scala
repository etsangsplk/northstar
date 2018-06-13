package io.jask.svc.northstar

import java.util.UUID

trait UUIDBinaryDeserializerOps {
  def deserialize(data: Array[Byte]): Option[UUID] = {
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
