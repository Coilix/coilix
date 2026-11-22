package dev.coilix.concurrency

import dev.coilix.annotation.EnumName
import dev.coilix.annotation.Enumerated

/** What the `fatalErrorHandler` bean does after a fatal error, set by `coilix.fatal-errors.action`. */
@Enumerated(ignoreCase = true)
enum FatalErrorAction {

  /** Closes the context and ends the process, see [[dev.coilix.concurrency.ShutdownFatalErrorHandler]]. */
  @EnumName("shutdown") case Shutdown

  /** Only logs the error, see [[dev.coilix.concurrency.LoggingFatalErrorHandler]]. */
  @EnumName("log") case Log
}
