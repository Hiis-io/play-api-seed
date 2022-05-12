package controllers

import com.mohiva.play.silhouette.api.exceptions.ProviderException
import com.mohiva.play.silhouette.api.{LoginEvent, LoginInfo, SignUpEvent, Silhouette}
import com.mohiva.play.silhouette.api.repositories.AuthInfoRepository
import com.mohiva.play.silhouette.api.services.{AuthenticatorService, AvatarService}
import com.mohiva.play.silhouette.api.util.{Clock, Credentials, PasswordHasherRegistry}
import com.mohiva.play.silhouette.impl.providers.CredentialsProvider
import io.swagger.annotations.{Api, ApiImplicitParam, ApiImplicitParams, ApiOperation}
import models.rest.{SignUp, UserCredentials, UserPassword}
import models.security._
import models.silhouette.Token
import play.api.libs.json.{JsError, Json}
import play.api.mvc.{AbstractController, ControllerComponents, Result}
import repositories.ExtendedAuthenticatorRepository
import services.UserService
import auth.{DefaultEnv, HasRole}

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

/**
 * Created by Abanda Ludovic on 12/05/2022.
 */


@Api(value = "Auth")
class AuthController @Inject() (
  components: ControllerComponents,
  userService: UserService,
  silhouette: Silhouette[DefaultEnv],
  credentialsProvider: CredentialsProvider,
  extendedAuthenticatorRepository: ExtendedAuthenticatorRepository,
  authInfoRepository: AuthInfoRepository,
  passwordHasherRegistry: PasswordHasherRegistry,
  avatarService: AvatarService)(implicit ex: ExecutionContext) extends AbstractController(components) {


  @ApiOperation(value = "Signup new user", response = classOf[User])
  @ApiImplicitParams(
    Array(
      new ApiImplicitParam(
        name = "X-Auth-Token",
        value = "User access token with Admin access",
        required = true,
        dataType = "string",
        paramType = "header"),
      new ApiImplicitParam(
        value = "SignUp",
        required = true,
        dataType = "models.rest.SignUp",
        paramType = "body")))
  def signUp = silhouette.SecuredAction.async(parse.json) { implicit request =>
    request.body.validate[SignUp].map { signUp =>
      val loginInfo = LoginInfo(CredentialsProvider.ID, signUp.identifier)
      userService.retrieve(loginInfo).flatMap {
        case None => /* user does not already exists */
          val user = User(
            None,
            loginInfo,
            loginInfo.providerKey,
            signUp.email,
            signUp.firstName,
            signUp.lastName,
            None,
            UserRoles.User,
            true)
          val passwordInfo = passwordHasherRegistry.current.hash(signUp.password)
          for {
            avatar <- avatarService.retrieveURL(signUp.email)
            _ <- userService.save(user.copy(avatarURL = avatar))
            _ <- authInfoRepository.add(loginInfo, passwordInfo)
            authenticator <- silhouette.env.authenticatorService.create(loginInfo)
            token <- silhouette.env.authenticatorService.init(authenticator)
            result <- silhouette.env.authenticatorService.embed(
              token,
              Ok(Json.toJson(Token(token = token, expiresOn = authenticator.expirationDateTime))))
          } yield {
            silhouette.env.eventBus.publish(SignUpEvent(user, request))
            silhouette.env.eventBus.publish(LoginEvent(user, request))
            result
          }
        case Some(_) => /* user already exists! */
          Future(Conflict)
      }
    }.recoverTotal {
      case error =>
        Future.successful(BadRequest)
    }
  }

  @ApiOperation(value = "Login and get authentication token", response = classOf[Token])
  @ApiImplicitParams(
    Array(
      new ApiImplicitParam(
        value = "Credentials",
        required = true,
        dataType = "models.rest.UserCredentials",
        paramType = "body")))
  def authenticate = Action.async(parse.json[UserCredentials]) { implicit request =>
    val credentials =
      Credentials(request.body.identifier, request.body.password)
    credentialsProvider
      .authenticate(credentials)
      .flatMap { loginInfo =>
        userService.retrieve(loginInfo).flatMap {
          case Some(user) if !user.activated =>
            //Send email to user which will enable them activate their account
            Future.successful(Ok)
          case Some(user) =>
            silhouette.env.authenticatorService
              .create(loginInfo)
              .map {
                case authenticator => authenticator
              }
              .flatMap { authenticator =>
                silhouette.env.eventBus.publish(LoginEvent(user, request))
                silhouette.env.authenticatorService
                  .init(authenticator)
                  .flatMap { token =>
                    silhouette.env.authenticatorService
                      .embed(
                        token,
                        Ok(Json.obj(
                          "user" -> user,
                          "token" -> Json.toJson(Token(
                            token,
                            expiresOn = authenticator.expirationDateTime)))))
                  }
              }
          case None =>
            Future.successful(NotFound)
        }
      }
      .recover {
        case _: ProviderException =>
          Forbidden
      }
  }

  @ApiOperation(value = "Logout", response = classOf[Result])
  @ApiImplicitParams(
    Array(
      new ApiImplicitParam(
        name = "X-Auth-Token",
        value = "User access token",
        required = true,
        dataType = "string",
        paramType = "header")))
  def logout() = silhouette.SecuredAction.async { implicit request =>
    for {
      _ <- extendedAuthenticatorRepository.remove(request.authenticator.id)
      _ <- silhouette.env.authenticatorService.discard(request.authenticator, Ok)
    } yield Ok
  }

  @ApiOperation(value = "Get user information", response = classOf[User])
  @ApiImplicitParams(
    Array(
      new ApiImplicitParam(
        name = "X-Auth-Token",
        value = "User access token",
        required = true,
        dataType = "string",
        paramType = "header")))
  def getUser() = silhouette.SecuredAction.async { implicit request =>
    userService.retrieve(request.authenticator.loginInfo).flatMap {
      case Some(user) =>
        for {
          _ <- extendedAuthenticatorRepository.removeExpired()
        } yield {
          Ok(Json.toJson(user))
        }
      case _ => Future.successful(NotFound)
    }
  }

  @ApiOperation(value = "Change specific non admin user's password by Admin", response = classOf[Result])
  @ApiImplicitParams(
    Array(
      new ApiImplicitParam(
        name = "X-Auth-Token",
        value = "User access token with Admin access",
        required = true,
        dataType = "string",
        paramType = "header"),
      new ApiImplicitParam(
        value = "SignUp",
        required = true,
        dataType = "models.rest.UserPassword",
        paramType = "body")))
  def changeUserPassword() = silhouette.SecuredAction(HasRole(UserRoles.Admin)).async(parse.json[UserPassword]) { implicit request =>
    val newPasswordHash = passwordHasherRegistry.current.hash(request.body.newPassword)
    val loginInfo = LoginInfo(CredentialsProvider.ID, request.body.username)
    userService.retrieve(loginInfo).flatMap {
      case Some(_) => for {
        _ <- authInfoRepository.update(loginInfo, newPasswordHash)
        _ <- extendedAuthenticatorRepository.remove(loginInfo)
      } yield Ok
      case _ => Future.successful(NotFound)
    }
  }

  @ApiOperation(value = "Change user password and logout", response = classOf[Result])
  @ApiImplicitParams(
    Array(
      new ApiImplicitParam(
        name = "X-Auth-Token",
        value = "User access token",
        required = true,
        dataType = "string",
        paramType = "header"),
      new ApiImplicitParam(
        value = "SignUp",
        required = true,
        dataType = "models.rest.UserPassword",
        paramType = "body")))
  def changePassword() = silhouette.SecuredAction.async(parse.json[UserPassword]) { implicit request =>
    val newPasswordHash = passwordHasherRegistry.current.hash(request.body.newPassword)
    val loginInfo = request.authenticator.loginInfo
    for {
      _ <- authInfoRepository.update(loginInfo, newPasswordHash)
      _ <- silhouette.env.authenticatorService.discard(request.authenticator, Ok)
      _ <- extendedAuthenticatorRepository.remove(loginInfo)
    } yield Ok
  }
}
