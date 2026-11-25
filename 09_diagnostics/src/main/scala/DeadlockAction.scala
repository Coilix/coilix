package dev.coilix.diagnostics

import dev.coilix.annotation.EnumName
import dev.coilix.annotation.Enumerated

/** What the diagnostics monitor does when it finds deadlocked threads, set by `coilix.diagnostics.threads.on-deadlock`. */
@Enumerated(ignoreCase = true)
enum DeadlockAction {

  /**
   * Hands a [[dev.coilix.diagnostics.ThreadDeadlockException]] to the [[dev.coilix.concurrency.FatalErrorHandler]], which by
   * default closes the context and ends the process, because deadlocked threads never recover.
   */
  @EnumName("shutdown") case Shutdown

  /** Only logs the deadlock and sets the `threads` health indicator to `Down`, so liveness fails. */
  @EnumName("report") case Report
}
