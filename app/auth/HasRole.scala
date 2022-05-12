package auth

import com.mohiva.play.silhouette.api.Authorization
import com.mohiva.play.silhouette.impl.authenticators.JWTAuthenticator
import models.security.User
import models.security.UserRoles.UserRole
import play.api.mvc.Request

import scala.concurrent.Future

/**
 * Created by Abanda Ludovic on 11/05/2022.
 */
case class HasRole(role: UserRole) extends Authorization[User, JWTAuthenticator] {
  override def isAuthorized[B](identity: User, authenticator: JWTAuthenticator)(implicit request: Request[B]) =
    Future.successful(identity.role == role)
}