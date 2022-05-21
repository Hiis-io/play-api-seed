package models

import com.mohiva.play.silhouette.api.LoginInfo
import org.joda.time.DateTime
import play.api.libs.json._



/**
 * Created by Abanda Ludovic on 21/05/2022.
 */

object TokenActions extends Enumeration {
  type TokenAction = Value
  val RESET_PASSWORD = Value(1)
  val ACCOUNT_VERIFICATION = Value(2)

  implicit val TokenActionsReads = Reads.enumNameReads(this)
}

/**
 * A token to authenticate a user against an endpoint for a short time period.
 *
 * @param tokenId The unique token ID.
 * @param userLoginInfo The unique ID of the user the token is associated with.
 * @param expiry The date-time the token expires.
 */
case class VerificationToken(id: String, action: TokenActions.TokenAction, userLoginInfo: LoginInfo, expiry: DateTime)

object VerificationToken {
  implicit val loginInfoFormat = Json.format[LoginInfo]

  implicit val jodaDateReads = JodaReads.jodaDateReads("yyyy-MM-dd'T'HH:mm:ss.SSZZ")
  implicit val jodaDateWrites = JodaWrites.jodaDateWrites("yyyy-MM-dd'T'HH:mm:ss.SSZZ")

  implicit val authTokenFormat = Json.format[VerificationToken]
}