package jobs

import akka.actor.{ActorRef, ActorSystem}
import com.mohiva.play.silhouette.api.util.Clock
import repositories.ExtendedAuthenticatorRepository
import utils.Logger

import javax.inject._
import scala.concurrent.duration._

/**
 * Created by Abanda Ludovic on 12/05/2022.
 */

class JWTAuthenticatorCleaner @Inject()(
  actorSystem: ActorSystem,
  executor: TasksCustomExecutionContext,
  extendedAuthenticatorRepository: ExtendedAuthenticatorRepository, clock: Clock) extends Logger {

  implicit val ex = executor
  actorSystem.scheduler.scheduleAtFixedRate(
    initialDelay = 0.microseconds,
    interval = 5.minutes) ( () => {
    val start = clock.now.getMillis
    val msg = new StringBuffer("\n")
    msg.append("=================================\n")
    msg.append("Start to cleanup auth tokens\n")
    msg.append("=================================\n")
    extendedAuthenticatorRepository.removeExpired().map { deleted =>
      val seconds = (clock.now.getMillis - start) / 1000
      msg.append("Total of %s auth tokens(s) were deleted in %s seconds".format(deleted.length, seconds)).append("\n")
      msg.append("=================================\n")
      msg.append("=================================\n")
      logger.debug(msg.toString)
    }.recover {
      case e =>
        msg.append("Couldn't cleanup auth tokens because of unexpected error\n")
        msg.append("=================================\n")
        logger.error(msg.toString, e)
    }
  })
}