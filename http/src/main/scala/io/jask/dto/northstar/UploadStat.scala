package io.jask.dto.northstar

case class UploadStat(count: Long = 0,
                      size: Long = 0,
                      failures: Long = 0,
                      maxRecord: Long = Long.MinValue)
