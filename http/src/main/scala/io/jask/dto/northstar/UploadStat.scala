package io.jask.dto.northstar

import java.util.UUID

case class UploadStat(id: UUID,
                      goodRecords: Long = 0,
                      badRecords: Long = 0,
                      size: Long = 0,
                      maxRecord: Long = Long.MinValue)
