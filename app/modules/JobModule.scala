package modules

import jobs.JWTAuthenticatorCleaner
import play.api.inject.SimpleModule
import play.api.inject._

/**
 * Created by Abanda Ludovic on 12/05/2022.
 */

class JobModule extends SimpleModule(
  bind[JWTAuthenticatorCleaner].toSelf.eagerly()
)
