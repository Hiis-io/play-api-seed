package jobs

import com.mohiva.play.silhouette.api.LoginInfo
import com.mohiva.play.silhouette.api.repositories.AuthInfoRepository
import com.mohiva.play.silhouette.api.util.PasswordHasherRegistry
import com.mohiva.play.silhouette.impl.providers.CredentialsProvider
import models.security.{User, UserRoles}
import play.api.inject.ApplicationLifecycle
import services.UserService
import utils.{AdminData, Logger}

import javax.inject._
import scala.concurrent.{ExecutionContext, Future}

/**
 * Created by Abanda Ludovic on 12/05/2022.
 */


/**
 * Include all startup code in this section
 */
class ApplicationStart @Inject()(
                                  lifecycle: ApplicationLifecycle,
                                  adminData: AdminData,
                                  userService: UserService,
                                  credentialsProvider: CredentialsProvider,
                                  authInfoRepository: AuthInfoRepository,
                                  passwordHasherRegistry: PasswordHasherRegistry)(implicit executionContext: ExecutionContext) extends Logger {

  lifecycle.addStopHook { () =>
    Future.successful(())
  }

  // Use this only if you are using role based authentication and you want to set default admin credentials

  val loginInfo = LoginInfo(credentialsProvider.id, adminData.username)
  val admin = User(
    id = None,
    loginInfo = loginInfo,
    username = adminData.username,
    email = adminData.email,
    firstName = adminData.firstName,
    lastName = adminData.lastName,
    avatarURL = None,
    role = UserRoles.Admin,
    activated = true)

  val password = passwordHasherRegistry.current.hash(adminData.password)
  userService.retrieve(loginInfo).flatMap {
    case None => for {
      _ <- userService.save(admin)
      _ <- authInfoRepository.add(loginInfo, password)
    } yield logger.debug("Admin user created")
    case Some(_) => Future.successful(logger.debug("Admin details available"))
  }
}