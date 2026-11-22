package dev.coilix.concurrency

import dev.coilix.annotation.Slf4s
import scala.util.control.NonFatal

/**
 * Tells fatal errors apart from failures that the application survives, and passes the fatal ones to a
 * [[dev.coilix.concurrency.FatalErrorHandler]].
 *
 * Fatal are `VirtualMachineError` (such as `OutOfMemoryError` and `StackOverflowError`) and `LinkageError`. After such an error
 * the state of the program cannot be trusted, so Coilix does not go on as if nothing happened.
 */
@Slf4s
object FatalErrors {

  /** Whether `failure` is a `VirtualMachineError` or a `LinkageError`. */
  def isFatal(failure: Throwable): Boolean =
    failure match {
      case _: VirtualMachineError | _: LinkageError => true
      case _                                        => false
    }

  /**
   * Runs `task` for a thread of Coilix: a failure the application survives is logged at `ERROR` level, a fatal error goes to
   * `handler`, and anything else, such as an `InterruptedException`, is rethrown.
   *
   * @param name
   *   the name of the executor or scheduler used in log messages
   */
  def guarded(name: String, handler: FatalErrorHandler)(task: => Unit): Unit =
    try {
      task
    } catch {
      case NonFatal(failure)           => log.error(s"Task on $name failed", failure)
      case failure if isFatal(failure) => handler.handle(Thread.currentThread, failure)
    }

  /**
   * An uncaught exception handler for threads: a fatal error goes to `handler`, any other exception is logged at `ERROR` level
   * with the name of the thread.
   */
  def uncaughtExceptionHandler(handler: FatalErrorHandler): Thread.UncaughtExceptionHandler =
    (thread, failure) => uncaught(handler, thread, failure)

  private def uncaught(handler: FatalErrorHandler, thread: Thread, failure: Throwable): Unit =
    if (isFatal(failure)) handler.handle(thread, failure)
    else log.error(s"Uncaught exception in thread ${thread.getName}", failure)
}
