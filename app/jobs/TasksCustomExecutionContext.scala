package jobs

import javax.inject.Inject

import akka.actor.ActorSystem
import play.api.libs.concurrent.CustomExecutionContext

/**
 * Created by Abanda Ludovic on 12/05/2022.
 */

class TasksCustomExecutionContext @Inject() (actorSystem: ActorSystem)
  extends CustomExecutionContext(actorSystem, "tasks-dispatcher")