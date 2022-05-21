package models.rest

import org.joda.time.DateTime
import play.api.libs.json.{JodaReads, JodaWrites, Json, OFormat, Reads, Writes}

import java.util.UUID

/**
 * Created by Abanda Ludovic on 21/05/2022.
 */


case class Todo (
                  title: String,
                  description: String,
                  startDate: DateTime,
                  isCompleted: Boolean = false,
                  userID: Option[String] = None,
                  id: Option[String] = Some(UUID.randomUUID().toString)
                ) {
  require(startDate.isAfter(DateTime.now()))
}

object Todo {

  implicit val jodaDateReads: Reads[DateTime] = JodaReads.jodaDateReads("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'")
  implicit val jodaDateWrites: Writes[DateTime] = JodaWrites.jodaDateWrites("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'")

  implicit val TodoFormat: OFormat[Todo] = Json.format[Todo]
}
