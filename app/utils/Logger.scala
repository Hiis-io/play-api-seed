package utils

/**
 * Created by Abanda Ludovic on 12/05/2022.
 */


/**
 * Implement this to get a named logger in scope.
 */
trait Logger {

  /**
   * A named logger instance.
   */
  val logger = play.api.Logger(s"io.hiis.${this.getClass.getName.stripSuffix("$")}")
}
