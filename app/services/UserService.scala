package services

import com.mohiva.play.silhouette.api.services.IdentityService
import models.security.User
import reactivemongo.api.commands.WriteResult

import scala.concurrent.Future

/**
 * Created by Abanda Ludovic on 11/05/2022.
 */

trait UserService extends IdentityService[User] {

  def save(user: User): Future[WriteResult]

  def update(user: User): Future[User]

  def verifyUsername(username: String): Future[Boolean]
}
