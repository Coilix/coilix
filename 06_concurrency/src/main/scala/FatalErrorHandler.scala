package dev.coilix.concurrency

/**
 * Reacts to a fatal error, such as an `OutOfMemoryError`, thrown in a thread of Coilix or, in an application started by
 * `Coilix.run`, in any thread.
 *
 * The concurrency module registers a `fatalErrorHandler` bean chosen by `coilix.fatal-errors.action`: a
 * [[dev.coilix.concurrency.ShutdownFatalErrorHandler]], which closes the context and ends the process, or a
 * [[dev.coilix.concurrency.LoggingFatalErrorHandler]], which only logs. An application can register its own handler marked
 * `@Primary`:
 *
 * ```scala
 * @Component
 * @Primary
 * final class AlertingFatalErrorHandler(pager: Pager, terminator: ProcessTerminator) extends FatalErrorHandler {
 *   override def handle(thread: Thread, failure: Throwable): Unit = {
 *     pager.alert(s"Fatal error in ${thread.getName}")
 *     terminator.halt(70)
 *   }
 * }
 * ```
 */
trait FatalErrorHandler {

  /**
   * Handles `failure`, thrown in `thread`. It is called in the failing thread, so it must not wait for that thread or for the
   * executor that runs it, and it must not throw.
   */
  def handle(thread: Thread, failure: Throwable): Unit
}

/** Ready-made [[dev.coilix.concurrency.FatalErrorHandler]]s. */
object FatalErrorHandler {

  /** Only logs fatal errors, used where no handler is given, such as an executor created outside a context. */
  val logOnly: FatalErrorHandler = LoggingFatalErrorHandler()
}
