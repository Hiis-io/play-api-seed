package jobs

import akka.actor.ActorSystem
import com.mohiva.play.silhouette.api.util.Clock
import services.VerificationTokenService
import utils.Logger

import javax.inject.Inject
import scala.concurrent.duration.DurationInt

/**
 * Created by Abanda Ludovic on 21/05/2022.
 */

class VerificationTokenCleaner @Inject()(
                                          actorSystem: ActorSystem,
                                          executor: TasksCustomExecutionContext,
                                          service: VerificationTokenService,
                                          clock: Clock) extends Logger {

  implicit val ex = executor
  actorSystem.scheduler.scheduleAtFixedRate(
    initialDelay = 0.microseconds,
    interval = 7.minutes) ( () => {
    val start = clock.now.getMillis
    val msg = new StringBuffer("\n")
    msg.append("=================================\n")
    msg.append("Start to cleanup verification tokens\n")
    msg.append("=================================\n")
    service.clean.map { deleted =>
      val seconds = (clock.now.getMillis - start) / 1000
      msg.append("Total of %s verification tokens(s) were deleted in %s seconds".format(deleted.length, seconds)).append("\n")
      msg.append("=================================\n")
      msg.append("=================================\n")
      logger.debug(msg.toString)
    }.recover {
      case e =>
        msg.append("Couldn't cleanup verification tokens because of unexpected error\n")
        msg.append("=================================\n")
        logger.error(msg.toString, e)
    }
  })
}