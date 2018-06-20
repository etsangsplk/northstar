package io.jask.dto.northstar

import de.heikoseeberger.akkahttpcirce.FailFastCirceSupport
import io.circe._
import io.circe.generic.semiauto._

trait JsonSupport extends FailFastCirceSupport {
  implicit val uploadStatEncoder: Encoder[UploadStat]         = deriveEncoder
  implicit val uploadStatDecoder: Decoder[UploadStat]         = deriveDecoder
  implicit val fileStatEncoder: Encoder[FileStat]             = deriveEncoder
  implicit val fileStatDecoder: Decoder[FileStat]             = deriveDecoder
  implicit val fileUploadStatEncoder: Encoder[FileUploadStat] = deriveEncoder
  implicit val fileUploadStatDecoder: Decoder[FileUploadStat] = deriveDecoder
}
