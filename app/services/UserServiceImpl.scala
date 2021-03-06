package services

import javax.inject.Inject

import com.mohiva.play.silhouette.api.LoginInfo
import models.security.User
import play.api.libs.json.Json
import play.modules.reactivemongo.ReactiveMongoApi
import reactivemongo.api.commands.WriteResult
import reactivemongo.play.json._
import reactivemongo.play.json.collection._

import scala.concurrent.{ ExecutionContext, Future }

/**
 * Created by Abanda Ludovic on 11/05/2022.
 */

class UserServiceImpl @Inject() (val reactiveMongoApi: ReactiveMongoApi)(implicit ex: ExecutionContext) extends UserService {

  def users = reactiveMongoApi.database.map(_.collection[JSONCollection]("users"))

  override def retrieve(loginInfo: LoginInfo): Future[Option[User]] =
    users.flatMap(_.find(Json.obj("username" -> loginInfo.providerKey)).one[User])

  override def save(user: User): Future[WriteResult] =
    users.flatMap(_.insert.one(user))

  override def update(user: User): Future[User] = users.flatMap(_.update.one(Json.obj("loginInfo" -> user.loginInfo), user))
    .flatMap(_ => Future.successful(user))

  override def verifyUsername(username: String): Future[Boolean] = {
    users.flatMap(_.find(Json.obj("username" -> username)).one[User]).flatMap{
      case Some(_) => Future.successful(true)
      case _ => Future.successful(false)
    }
  }
}
