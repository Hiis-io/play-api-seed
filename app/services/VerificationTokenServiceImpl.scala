package services

import com.mohiva.play.silhouette.api.LoginInfo
import com.mohiva.play.silhouette.api.util.Clock
import models.TokenActions.TokenAction
import models.VerificationToken
import org.joda.time.{DateTime, DateTimeZone}
import play.api.libs.json.{JsObject, Json}
import play.modules.reactivemongo.ReactiveMongoApi
import reactivemongo.api.Cursor

import javax.inject.Inject
import scala.concurrent.duration.FiniteDuration
import scala.concurrent.{ExecutionContext, Future}
import reactivemongo.play.json._
import reactivemongo.play.json.collection._

import java.util.UUID

/**
 * Created by Abanda Ludovic on 21/05/2022.
 */

class VerificationTokenServiceImpl @Inject()(val reactiveMongoApi: ReactiveMongoApi, clock: Clock)(implicit ex: ExecutionContext)extends VerificationTokenService {

  /**
   * The data store for the Verification tokens.
   */
  private def verificationTokenCollection = reactiveMongoApi.database.map(_.collection[JSONCollection]("verificationTokens"))

  /**
   * Creates a new verification token and saves it in the backing store.
   *
   * @param action The action permitted by this token
   * @param loginInfo The user ID for which the token should be created.
   * @param expiry    The duration a token expires.
   * @return The saved verification token.
   */
  override def create(action: TokenAction, loginInfo: LoginInfo, expiry: FiniteDuration): Future[VerificationToken] = {
    val token = VerificationToken(UUID.randomUUID().toString, action, loginInfo, clock.now.withZone(DateTimeZone.UTC).plusSeconds(expiry.toSeconds.toInt))
    verificationTokenCollection.flatMap(_.insert.one(token)).flatMap(_ => Future.successful(token))
  }

  /**
   * Validates a token ID.
   *
   * @param id The token ID to validate.
   * @return The token if it's valid, None otherwise.
   */
  override def find(id: String): Future[Option[VerificationToken]] = verificationTokenCollection.flatMap(_.find(Json.obj("id" -> id), projection = Option.empty[JsObject]).one[VerificationToken])

  /**
   * Deletes a token ID.
   *
   * @param id The token ID to delete.
   * @return Unit
   */
  def delete(id: String): Future[Unit] = verificationTokenCollection.flatMap(_.delete.one(Json.obj("id" -> id))).flatMap(_ => Future.successful(()))

  /**
   * Finds expired tokens.
   *
   * @param date The date to be used when comparing.
   * @return The tokens if it's valid, List.empty otherwise.
   */
  def findExpired(date: DateTime): Future[List[VerificationToken]] = verificationTokenCollection.flatMap(_.find(Json.obj(), projection = Option.empty[JsObject]).cursor[VerificationToken]().collect[List](Int.MaxValue, Cursor.FailOnError[List[VerificationToken]]()))
    .flatMap(tokens => Future.successful(tokens.filter(token => token.expiry.isBefore(date))))

  /**
   * Cleans expired tokens.
   *
   * @return The list of deleted tokens.
   */
  override def clean: Future[List[VerificationToken]] = findExpired(clock.now.withZone(DateTimeZone.UTC)).flatMap { tokens =>
    Future.sequence(tokens.map { token =>
      delete(token.id).map(_ => token)
    })
  }
}
