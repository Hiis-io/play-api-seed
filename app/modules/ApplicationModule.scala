package modules

import com.google.inject.name.Named
import com.google.inject.{AbstractModule, Provides}
import com.mohiva.play.silhouette.api.actions.{SecuredErrorHandler, UnsecuredErrorHandler}
import com.mohiva.play.silhouette.api.crypto.{Crypter, CrypterAuthenticatorEncoder}
import com.mohiva.play.silhouette.api.repositories.{AuthInfoRepository, AuthenticatorRepository}
import com.mohiva.play.silhouette.api.services.{AuthenticatorService, AvatarService}
import com.mohiva.play.silhouette.api.util._
import com.mohiva.play.silhouette.api.{Environment, EventBus, Silhouette, SilhouetteProvider}
import com.mohiva.play.silhouette.crypto.{JcaCrypter, JcaCrypterSettings}
import com.mohiva.play.silhouette.impl.authenticators._
import com.mohiva.play.silhouette.impl.services.GravatarService
import com.mohiva.play.silhouette.impl.util.{PlayCacheLayer, SecureRandomIDGenerator}
import com.mohiva.play.silhouette.password.{BCryptPasswordHasher, BCryptSha256PasswordHasher}
import com.mohiva.play.silhouette.persistence.daos.DelegableAuthInfoDAO
import com.mohiva.play.silhouette.persistence.repositories.DelegableAuthInfoRepository
import daos._
import net.ceedubs.ficus.Ficus._
import net.ceedubs.ficus.readers.ArbitraryTypeReader._
import net.ceedubs.ficus.readers.EnumerationReader._
import net.codingwell.scalaguice.ScalaModule
import play.api.Configuration
import play.api.libs.ws.WSClient
import play.modules.reactivemongo.ReactiveMongoApi
import repositories.{AuthenticatorRepositoryImpl, ExtendedAuthenticatorRepository}
import services.{UserService, UserServiceImpl}
import auth.{CustomSecuredErrorHandler, CustomUnsecuredErrorHandler, DefaultEnv}
import jobs.ApplicationStart
import utils.{AdminData, Logger}

import scala.concurrent.ExecutionContext.Implicits.global

/**
 * Created by Abanda Ludovic on 11/05/2022.
 */

class ApplicationModule extends AbstractModule with ScalaModule{

  override def configure() = {
    bind[Silhouette[DefaultEnv]].to[SilhouetteProvider[DefaultEnv]]
    bind[UnsecuredErrorHandler].to[CustomUnsecuredErrorHandler]
    bind[SecuredErrorHandler].to[CustomSecuredErrorHandler]
    bind[UserService].to[UserServiceImpl]
    bind[CacheLayer].to[PlayCacheLayer]
    bind[IDGenerator].toInstance(new SecureRandomIDGenerator())
    bind[EventBus].toInstance(EventBus())
    bind[Clock].toInstance(Clock())
    bind[ExtendedAuthenticatorRepository].to[AuthenticatorRepositoryImpl]
    bind(classOf[ApplicationStart]).asEagerSingleton()
  }

  @Provides
  def providePasswordDAO(reactiveMongoApi: ReactiveMongoApi): DelegableAuthInfoDAO[PasswordInfo] = new PasswordInfoDAO(reactiveMongoApi)

  /**
   * Provides the HTTP layer implementation.
   *
   * @param client Play's WS client.
   * @return The HTTP layer implementation.
   */
  @Provides
  def provideHTTPLayer(client: WSClient): HTTPLayer = new PlayHTTPLayer(client)

  /**
   * Provides the Silhouette environment.
   *
   * @param userService          The user service implementation.
   * @param authenticatorService The authentication service implementation.
   * @param eventBus             The event bus instance.
   * @return The Silhouette environment.
   */
  @Provides
  def provideEnvironment(
                          userService: UserService,
                          authenticatorService: AuthenticatorService[JWTAuthenticator],
                          eventBus: EventBus): Environment[DefaultEnv] =
    Environment[DefaultEnv](userService, authenticatorService, Seq(), eventBus)

  /**
   * Provides the crypter for the authenticator.
   *
   * @param configuration The Play configuration.
   * @return The crypter for the authenticator.
   */
  @Provides
  @Named("authenticator-crypter")
  def provideAuthenticatorCrypter(configuration: Configuration): Crypter = {
    val config = configuration.underlying.as[JcaCrypterSettings]("silhouette.authenticator.crypter")

    new JcaCrypter(config)
  }

  /**
   * Provides the JWT authenticator settings.
   *
   * @param configuration        The Play configuration.
   * @return The JWT authenticator settings.
   */
  @Provides
  def provideJWTAuthenticatorSettings(configuration: Configuration): JWTAuthenticatorSettings = {
    configuration.underlying.as[JWTAuthenticatorSettings]("silhouette.JWTAuthenticatorSettings")
  }


  /**
   * Provides the authenticator service.
   *
   * @param crypter              The crypter implementation.
   * @param idGenerator          The ID generator implementation.
   * @param configuration        The Play configuration.
   * @param clock                The clock instance.
   * @param settings             The JWT Authenticator Settings
   * @return The authenticator service.
   */
  @Provides
  def provideAuthenticatorService(
                                   @Named("authenticator-crypter") crypter: Crypter,
                                   idGenerator: IDGenerator,
                                   configuration: Configuration,
                                   clock: Clock,
                                   settings: JWTAuthenticatorSettings,
                                   reactiveMongoApi: ReactiveMongoApi): AuthenticatorService[JWTAuthenticator] = {
    val encoder = new CrypterAuthenticatorEncoder(crypter)
    val authenticatorRepository = new AuthenticatorRepositoryImpl(reactiveMongoApi, clock)

    new JWTAuthenticatorService(settings, Some(authenticatorRepository), encoder, idGenerator, clock)
  }

  /**
   * Provides the password hasher registry.
   *
   * @return The password hasher registry.
   */
  @Provides
  def providePasswordHasherRegistry(): PasswordHasherRegistry = {
    PasswordHasherRegistry(new BCryptSha256PasswordHasher(), Seq(new BCryptPasswordHasher()))
  }

  /**
   * Provides the auth info repository.
   *
   * @param passwordInfoDAO The implementation of the delegable password auth info DAO.
   * @return The auth info repository instance.
   */
  @Provides
  def provideAuthInfoRepository(passwordInfoDAO: DelegableAuthInfoDAO[PasswordInfo]): AuthInfoRepository =
    new DelegableAuthInfoRepository(passwordInfoDAO)

  /**
   * Provides the avatar service.
   *
   * @param httpLayer The HTTP layer implementation.
   * @return The avatar service implementation.
   */
  @Provides
  def provideAvatarService(httpLayer: HTTPLayer): AvatarService = new GravatarService(httpLayer)

  /**
   * Provides default platform admin settings.
   *
   * @param configuration        The Play configuration.
   * @return The default admin data settings.
   */
  @Provides
  def provideAdminData(configuration: Configuration): AdminData = {
    configuration.underlying.as[AdminData]("admin")
  }
}
