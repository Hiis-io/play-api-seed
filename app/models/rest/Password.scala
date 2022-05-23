package models.rest

import play.api.libs.json.{Json, OFormat}

/**
 * Created by Abanda Ludovic on 22/05/2022.
 */

case class Password (password: String)

object Password {
  implicit val passwordFormat: OFormat[Password] = Json.format[Password]
}
