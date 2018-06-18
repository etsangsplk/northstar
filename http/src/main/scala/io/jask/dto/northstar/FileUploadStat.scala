package io.jask.dto.northstar

case class FileUploadStat(uploadStat: Option[UploadStat] = None,
                          fileStat: Option[FileStat] = None)
