package io.jask.dto.northstar

import akka.http.scaladsl.marshallers.sprayjson.SprayJsonSupport
import spray.json.DefaultJsonProtocol

trait JsonSupport extends SprayJsonSupport with DefaultJsonProtocol {
  implicit val uploadStatFormat = jsonFormat4(UploadStat)
  implicit val fileStatFormat = jsonFormat2(FileStat)
  implicit val fileUploadStatFormat = jsonFormat2(FileUploadStat)
}
