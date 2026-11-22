package dev.coilix.concurrency

import dev.coilix.annotation.Slf4s

/**
 * A [[dev.coilix.concurrency.FatalErrorHandler]] that logs fatal errors at `ERROR` level and lets the application go on.
 * `coilix.fatal-errors.action = log` selects it.
 */
@Slf4s
final class LoggingFatalErrorHandler extends FatalErrorHandler {

  override def handle(thread: Thread, failure: Throwable): Unit =
    log.error(s"Fatal error in thread ${thread.getName}, the application goes on", failure)
}
