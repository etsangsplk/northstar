package io.jask.dto.northstar

import io.circe.Json

case class Envelope(dataType: String, recordNum: Long, message: Json)
