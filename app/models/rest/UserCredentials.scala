package models.rest

import play.api.libs.json.{Json, OFormat}

/**
 * Created by Abanda Ludovic on 11/05/2022.
 */

final case class UserCredentials(
                                identifier: String,
                                password: String
                                )
object UserCredentials {
  implicit val userCredentialsFormat: OFormat[UserCredentials] = Json.format[UserCredentials]
}