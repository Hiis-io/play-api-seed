package repositories

import com.mohiva.play.silhouette.api.LoginInfo

import javax.inject.Inject
import com.mohiva.play.silhouette.api.util.Clock
import com.mohiva.play.silhouette.impl.authenticators.JWTAuthenticator
import models.silhouette.JWTAuthenticatorFormat._
import org.joda.time.DateTimeZone
import play.api.libs.json.{ JsObject, Json }
import play.api.libs.json.Writes._
import reactivemongo.api.{ Cursor, ReadPreference }
import reactivemongo.play.json.collection._
import play.modules.reactivemongo._
import reactivemongo.play.json._

import scala.language.postfixOps
import scala.concurrent.duration._
import scala.concurrent.{ ExecutionContext, Future }

/**
 * Created by Abanda Ludovic on 11/05/2022.
 */

/**
 * Implementation of the authenticator repository which uses the database layer to persist the authenticator.
 */
class AuthenticatorRepositoryImpl @Inject() (reactiveMongoApi: ReactiveMongoApi, clock: Clock)(implicit ec: ExecutionContext) extends ExtendedAuthenticatorRepository {

  final val maxDuration = 12 hours
  /**
   * The data store for the password info.
   */
  def authenticators = reactiveMongoApi.database.map(_.collection[JSONCollection]("authenticators"))

  /**
   * Finds the authenticator for the given ID.
   *
   * @param id The authenticator ID.
   * @return The found authenticator or None if no authenticator could be found for the given ID.
   */
  override def find(id: String): Future[Option[JWTAuthenticator]] = {
    authenticators.flatMap(_.find(Json.obj("_id" -> id)).one[JWTAuthenticator])
  }

  /**
   * Adds a new authenticator.
   *
   * @param authenticator The authenticator to add.
   * @return The added authenticator.
   */
  override def add(authenticator: JWTAuthenticator): Future[JWTAuthenticator] = {
    val passInfo = Json.obj("_id" -> authenticator.id, "authenticator" -> authenticator, "duration" -> maxDuration)
    authenticators.flatMap(_.insert.one(passInfo)).flatMap(_ => Future(authenticator))
  }

  /**
   * Updates an already existing authenticator.
   *
   * @param authenticator The authenticator to update.
   * @return The updated authenticator.
   */
  override def update(authenticator: JWTAuthenticator) = {
    val passInfo = Json.obj("_id" -> authenticator.id, "authenticator" -> authenticator, "duration" -> maxDuration)
    authenticators.flatMap(_.update.one(Json.obj("_id" -> authenticator.id), passInfo)).flatMap(_ => Future(authenticator))
  }

  /**
   * Removes the authenticator for the given ID.
   *
   * @param id The authenticator ID.
   * @return An empty future.
   */
  override def remove(id: String): Future[Unit] =
    authenticators.flatMap(_.delete.one(Json.obj("_id" -> id))).flatMap(_ => Future.successful(()))

  /**
   * Removes all the authenticators of a given loginInfo.
   *
   * @param loginInfo The loginInfo of the user.
   * @return An empty future.
   */
  override def remove(loginInfo: LoginInfo): Future[Unit] = {
    authenticators.flatMap(_.delete.one(Json.obj("loginInfo" -> loginInfo))).flatMap(_ => Future.successful(Unit))
  }

  /**
   * Removes all the expired authenticators.
   *
   * @return An empty future.
   */
  override def removeExpired(): Future[List[JWTAuthenticator]] = {
    val now = clock.now.withZone(DateTimeZone.UTC)
    authenticators.flatMap(_.find(Json.obj(), projection = Option.empty[JsObject])
      .cursor[JWTAuthenticator](ReadPreference.Primary)
      .collect[List](Int.MaxValue, Cursor.FailOnError[List[JWTAuthenticator]]()))
      .flatMap(jwtAuthenticators => Future.successful(jwtAuthenticators.filter(e => e.expirationDateTime.isBefore(now))))
      .flatMap(filteredElements => Future.sequence(filteredElements.map(jwtAuthenticator =>
        remove(jwtAuthenticator.id).map(_ => jwtAuthenticator))))
  }
}