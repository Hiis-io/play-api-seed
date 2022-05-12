package models.rest

import play.api.libs.json
import play.api.libs.json.{ Json, OFormat }

/**
 * Created by Abanda Ludovic on 12/05/2022.
 */

case class UserPassword(
  username: String,
  newPassword: String)

object UserPassword {
  implicit val userPasswordOFormat: OFormat[UserPassword] = Json.format[UserPassword]
}
