package models.security

import play.api.libs.json.Reads

/**
 * Created by Abanda Ludovic on 11/05/2022.
 */

object UserRoles extends Enumeration {
  type UserRole = Value
  val User = Value(1)
  val Admin = Value(2)

  implicit val UserRolesRead = Reads.enumNameReads(this)
}