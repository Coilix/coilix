package dev.coilix.concurrency

import dev.coilix.annotation.Slf4s
import dev.coilix.context.ConfigurableApplicationContext
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicBoolean
import scala.util.control.NonFatal

/**
 * A [[dev.coilix.concurrency.FatalErrorHandler]] that ends the application after the first fatal error, the default one,
 * selected by `coilix.fatal-errors.action = shutdown`.
 *
 * It logs the error, then closes the context and exits with `exitCode` on a new thread named `coilix-fatal-shutdown`, because
 * the failing thread may belong to an executor that closing waits for. A daemon thread `coilix-fatal-watchdog` halts the
 * process without running the shutdown hooks when closing and exiting take longer than `shutdownTimeout`, for example when
 * threads are deadlocked. Fatal errors that follow the first one are only logged.
 *
 * It marks the first fatal error with an atomic, because it runs on the failing thread, which may be a thread of a broken
 * runtime that can no longer run fibers.
 *
 * @param settings
 *   the exit code and the time limit of closing
 * @param context
 *   the context to close
 * @param terminator
 *   ends the process
 */
@Slf4s
@SuppressWarnings(Array("dev.coilix.warts.ThreadPrimitives"))
final class ShutdownFatalErrorHandler(
  settings:   FatalErrorSettings,
  context:    ConfigurableApplicationContext,
  terminator: ProcessTerminator
) extends FatalErrorHandler {

  private val started  = AtomicBoolean(false)
  private val finished = CountDownLatch(1)

  override def handle(thread: Thread, failure: Throwable): Unit =
    if (started.compareAndSet(false, true)) shutDown(thread, failure)
    else report(s"Fatal error in thread ${thread.getName} while shutting down after an earlier one", Some(failure))

  /** Whether a fatal error has started the shutdown. */
  def isShuttingDown: Boolean = started.get

  private def shutDown(thread: Thread, failure: Throwable): Unit = {
    report(s"Fatal error in thread ${thread.getName}, shutting down with exit code ${settings.exitCode}", Some(failure))
    startThread("coilix-fatal-watchdog", daemon = true)(watch())
    startThread("coilix-fatal-shutdown", daemon = false)(closeAndExit())
  }

  private def closeAndExit(): Unit = {
    try {
      context.close()
    } catch {
      case NonFatal(closing) => log.error("Closing the context after a fatal error failed", closing)
    }
    terminator.exit(settings.exitCode)
    finished.countDown()
  }

  private def watch(): Unit =
    if (!finished.await(settings.shutdownTimeout.toMillis, TimeUnit.MILLISECONDS)) {
      report(s"Closing after a fatal error took longer than ${settings.shutdownTimeout}, halting", None)
      terminator.halt(settings.exitCode)
    }

  private def startThread(name: String, daemon: Boolean)(body: => Unit): Unit = {
    val thread = Thread(() => body, name)
    thread.setDaemon(daemon)
    thread.start()
  }

  private def report(message: String, failure: Option[Throwable]): Unit =
    try {
      failure.fold(log.error(message))(log.error(message, _))
    } catch {
      case _: Throwable => ()
    }
}
