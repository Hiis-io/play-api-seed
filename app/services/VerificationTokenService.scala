package services

import com.mohiva.play.silhouette.api.LoginInfo
import models.TokenActions.TokenAction
import models.VerificationToken
import org.joda.time.DateTime

import scala.concurrent.Future
import scala.concurrent.duration.{DurationInt, FiniteDuration}
import scala.language.postfixOps

/**
 * Created by Abanda Ludovic on 21/05/2022.
 */

trait VerificationTokenService {

  /**
   * Creates a new auth token and saves it in the backing store.
   *
   * @param action The action permitted by this token
   * @param loginInfo The user ID for which the token should be created.
   * @param expiry The duration a token expires.
   * @return The saved verification token.
   */
  def create(action: TokenAction, loginInfo: LoginInfo, expiry: FiniteDuration = 5 minutes): Future[VerificationToken]


  /**
   * Finds a token ID.
   *
   * @param id The token ID to validate.
   * @return The token if it's valid, None otherwise.
   */
  def find(id: String): Future[Option[VerificationToken]]

  /**
   * Deletes a token ID.
   *
   * @param id The token ID to delete.
   * @return Unit
   */
  def delete(id: String): Future[Unit]

  /**
   * Finds expired tokens.
   *
   * @param date The date to be used when comparing.
   * @return The tokens if it's valid, List.empty otherwise.
   */
  def findExpired(date: DateTime): Future[List[VerificationToken]]

  /**
   * Cleans expired tokens.
   *
   * @return The list of deleted tokens.
   */
  def clean: Future[List[VerificationToken]]
}
