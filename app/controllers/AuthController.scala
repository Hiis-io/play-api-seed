package controllers

import com.mohiva.play.silhouette.api.exceptions.ProviderException
import com.mohiva.play.silhouette.api.{LoginEvent, LoginInfo, SignUpEvent, Silhouette}
import com.mohiva.play.silhouette.api.repositories.AuthInfoRepository
import com.mohiva.play.silhouette.api.services.{AuthenticatorService, AvatarService}
import com.mohiva.play.silhouette.api.util.{Clock, Credentials, PasswordHasherRegistry, PasswordInfo}
import com.mohiva.play.silhouette.impl.providers.CredentialsProvider
import io.swagger.annotations.{Api, ApiImplicitParam, ApiImplicitParams, ApiOperation}
import models.rest.{Password, SignUp, UserCredentials, UserPassword}
import models.security._
import models.silhouette.Token
import play.api.libs.json.{JsError, Json}
import play.api.mvc.{AbstractController, AnyContent, ControllerComponents, Request, Result}
import repositories.ExtendedAuthenticatorRepository
import services.{UserService, VerificationTokenService}
import auth.{DefaultEnv, HasRole}
import models.TokenActions
import play.api.i18n.{I18nSupport, Messages}
import play.api.libs.mailer.{Email, MailerClient}

import java.net.URLDecoder
import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

/**
 * Created by Abanda Ludovic on 12/05/2022.
 */


@Api(value = "Auth")
class AuthController @Inject()(
                                components: ControllerComponents,
                                userService: UserService,
                                silhouette: Silhouette[DefaultEnv],
                                credentialsProvider: CredentialsProvider,
                                extendedAuthenticatorRepository: ExtendedAuthenticatorRepository,
                                authInfoRepository: AuthInfoRepository,
                                passwordHasherRegistry: PasswordHasherRegistry,
                                avatarService: AvatarService,
                                mailerClient: MailerClient,
                                verificationTokenService: VerificationTokenService)(implicit ex: ExecutionContext) extends AbstractController(components) with I18nSupport {


  //This endpoint is just for simplicity since it is prone to attacks. You should consider exposing this to trusted frontends only. CORS is a good start but doesn't work well with mobile apps
  @ApiOperation(value = "Verify if a username is available or not", response = classOf[Boolean])
  def checkUsername(username: String) = silhouette.UnsecuredAction.async { implicit request =>
    userService.verifyUsername(username).flatMap {
      case false => Future.successful(Ok(Json.obj("message" -> "Username can be used")))
      case true => Future.successful(Conflict(Json.obj("message" -> "Username is taken already")))
    }
  }

  @ApiOperation(value = "Signup new user", response = classOf[User])
  @ApiImplicitParams(
    Array(
      new ApiImplicitParam(
        value = "SignUp",
        required = true,
        dataType = "models.rest.SignUp",
        paramType = "body")))
  def signUp = silhouette.UnsecuredAction.async(parse.json) { implicit request =>
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
            activated = false)
          val passwordInfo = passwordHasherRegistry.current.hash(signUp.password)
          for {
            avatar <- avatarService.retrieveURL(signUp.email)
            _ <- userService.save(user.copy(avatarURL = avatar))
            _ <- authInfoRepository.add(loginInfo, passwordInfo)
            verificationToken <- verificationTokenService.create(TokenActions.ACCOUNT_VERIFICATION, loginInfo)
          } yield {
            //This should be a dynamic link that contains the account activation token and can be understood by the frontend
            //The frontend should be able to extract the token from the request
            //This token can then be sent to the backend inorder to activate the account
            val url = routes.AuthController.activateAccount(verificationToken.id).absoluteURL()

            mailerClient.send(Email(
              subject = "Welcome",
              from = "dev@hiis.io",
              to = Seq(URLDecoder.decode(user.email, "UTF-8")),
              bodyText = Some(views.txt.emails.signUp(user, url).body)
            ))
            Ok(Json.obj("message" -> "Follow the link sent to your email to verify your account"))
          }
        case Some(_) => Future(Conflict)
      }
    }.recoverTotal(_ => Future.successful(BadRequest))
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
              .flatMap { authenticator =>
                silhouette.env.eventBus.publish(LoginEvent(user, request))
                silhouette.env.authenticatorService.init(authenticator).flatMap { token =>
                  silhouette.env.authenticatorService.embed(
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
        case error: ProviderException =>
          Forbidden(s"$error")
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
      case Some(user) => Future.successful(Ok(Json.toJson(user)))
      case _ => Future.successful(NotFound)
    }
  }

  @ApiOperation(value = "Change specific non admin user's password by Admin", response = classOf[Result])
  @ApiImplicitParams(
    Array(
      new ApiImplicitParam(
        name = "X-Auth-Token",
        value = "User access token with admin role",
        required = true,
        dataType = "string",
        paramType = "header"),
      new ApiImplicitParam(
        value = "Password change",
        required = true,
        dataType = "models.rest.UserPassword",
        paramType = "body")))
  def changeUserPassword() = silhouette.SecuredAction(HasRole(UserRoles.Admin)).async(parse.json[UserPassword]) { implicit request =>
    //This is just for demonstration purpose.
    //This shows how you can use the user's role to authorize an actions
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
        value = "Password change",
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

  @ApiOperation(value = "Resend activation link to user email", response = classOf[String])
  def sendVerification(username: String) = Action.async { implicit request: Request[AnyContent] =>
    val loginInfo = LoginInfo(CredentialsProvider.ID, username)
    userService.retrieve(loginInfo).flatMap {
      case Some(user) if !user.activated && user.loginInfo.providerID == CredentialsProvider.ID =>
        verificationTokenService.create(TokenActions.ACCOUNT_VERIFICATION, loginInfo).flatMap { token =>
          //This should be a dynamic link that contains the account activation token and can be understood by the frontend
          //The frontend should be able to extract the token from the request
          //This token can then be sent to the backend inorder to activate the account
          val url = routes.AuthController.activateAccount(token.id).absoluteURL()

          Future {
            mailerClient.send(Email(
              subject = "Activate your account",
              from = "dev@hiis.io",
              to = Seq(URLDecoder.decode(user.email, "UTF-8")),
              bodyText = Some(views.txt.emails.activateAccount(user, url).body)
            ))
          }.flatMap(_ => Future.successful(Ok(Json.obj("message" -> "Follow the link sent to your email to verify your account"))))
        }
      case _ => Future.successful(BadRequest)
    }
  }


  @ApiOperation(value = "Activate account using activation token sent to email", response = classOf[String])
  def activateAccount(token: String) = Action.async { implicit request: Request[AnyContent] =>
    verificationTokenService.find(token).flatMap {
      case Some(token) if token.action == TokenActions.ACCOUNT_VERIFICATION => userService.retrieve(token.userLoginInfo).flatMap {
        case Some(user) =>
          userService.update(user.copy(activated = true)).flatMap(_ => Future.successful(Ok(Json.obj("message" -> "You can now login into your account"))))
        case _ => Future.successful(BadRequest)
      }
      case None => Future.successful(BadRequest)
    }
  }

  @ApiOperation(value = "Send reset password token to user email", response = classOf[String])
  def sendPasswordReset(userName: String) = Action.async { implicit request =>
    val loginInfo = LoginInfo(CredentialsProvider.ID, userName)
    userService.retrieve(loginInfo).flatMap {
      case Some(user) =>
        verificationTokenService.create(TokenActions.RESET_PASSWORD ,user.loginInfo).map { authToken =>
          //This should be a dynamic link that contains the reset token and can be understood by the frontend
          //The frontend should be able to extract the token from the request and require the user to enter a new password
          //This token and new password can then be sent to the backend inorder to change the password
          val url = routes.AuthController.resetPassword(authToken.id).absoluteURL()

          mailerClient.send(Email(
            subject = "Password Reset",
            from = "dev@hiis.io",
            to = Seq(user.email),
            bodyText = Some(views.txt.emails.resetPassword(user, url).body)
          ))
          Ok(Json.toJson("message" -> Messages("reset.email.sent")))
        }
      case None =>
        //This should always return Ok, since some attackers might use this endpoint to verify which usernames are registered in the system. By retuning anything other than Ok you give them access to that information
        Future.successful(Ok(Json.toJson("message" -> Messages("reset.email.sent"))))
    }
  }

  @ApiOperation(value = "Reset password using token sent to email", response = classOf[String])
  @ApiImplicitParams(
    Array(
      new ApiImplicitParam(
        value = "Password",
        required = true,
        dataType = "models.rest.Password",
        paramType = "body"
      )
    )
  )
  def resetPassword(token: String) = Action.async(parse.json) { implicit request =>
    request.body.validate[Password].map { passwordData =>
      verificationTokenService.find(token).flatMap {
        case Some(authToken) if authToken.action == TokenActions.RESET_PASSWORD =>
          userService.retrieve(authToken.userLoginInfo).flatMap {
            case Some(user) if user.loginInfo.providerID == CredentialsProvider.ID =>
              val authInfo = passwordHasherRegistry.current.hash(passwordData.password)
              authInfoRepository.update[PasswordInfo](user.loginInfo, authInfo).flatMap { _ =>
                Future.successful(Ok(Json.obj("message" -> "Password reset successful")))
              }
            case _ => Future.successful(BadRequest)
          }
        case None => Future.successful(BadRequest)
      }
    }.recoverTotal(error =>
      Future.successful(BadRequest))
  }
}
