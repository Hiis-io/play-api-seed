package repositories

import com.mohiva.play.silhouette.api.LoginInfo
import com.mohiva.play.silhouette.api.repositories.AuthenticatorRepository
import com.mohiva.play.silhouette.impl.authenticators.JWTAuthenticator

import scala.concurrent.Future


/**
 * Created by Abanda Ludovic on 11/05/2022.
 */

trait ExtendedAuthenticatorRepository extends AuthenticatorRepository[JWTAuthenticator] {
  /**
   * Removes all the authenticators a given loginInfo.
   *
   * @param loginInfo The loginInfo of the user.
   * @return An empty future.
   */
  def remove(loginInfo: LoginInfo): Future[Unit]

  /**
   * Removes all the expired authenticators.
   *
   * @return An empty future.
   */
  def removeExpired(): Future[List[JWTAuthenticator]]
}
