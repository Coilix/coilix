package dev.coilix.concurrency

import dev.coilix.common.CoilixException

/**
 * Thrown at startup when a method annotated with [[dev.coilix.annotation.Scheduled]] cannot be scheduled, for example because
 * it takes parameters or sets both `fixedRate` and `fixedDelay`. The message names the bean and the method.
 */
final class InvalidScheduledMethodException(message: String, cause: Option[Throwable] = None) extends CoilixException(message) {
  cause.foreach(initCause)
}
