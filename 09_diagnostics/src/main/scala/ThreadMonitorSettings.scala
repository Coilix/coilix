package dev.coilix.diagnostics

import scala.concurrent.duration.DurationInt
import scala.concurrent.duration.FiniteDuration

/**
 * Settings of the checks of platform threads, read from `coilix.diagnostics.threads`. Every key is optional.
 *
 * @param enabled
 *   whether the thread checks run; the health indicators are refreshed either way
 * @param onDeadlock
 *   what happens when threads are deadlocked
 * @param warningCount
 *   the number of platform threads from which [[dev.coilix.diagnostics.ThreadCountCheck]] warns
 * @param criticalCount
 *   the number of platform threads from which the finding is `Critical`
 * @param blockedAfter
 *   how long a thread may be blocked on a monitor before [[dev.coilix.diagnostics.LongBlockedCheck]] warns
 * @param growthWindow
 *   the window in which [[dev.coilix.diagnostics.ThreadGrowthCheck]] measures the growth of the thread count
 * @param growthLimit
 *   the growth within `growthWindow` from which it warns
 * @param startupGrace
 *   how long after the start the growth is not checked
 * @param stackDepth
 *   how many frames of each stack are logged
 * @param repeatAfter
 *   how long a warning of one kind stays quiet after it was logged
 */
final case class ThreadMonitorSettings(
  enabled:       Boolean        = true,
  onDeadlock:    DeadlockAction = DeadlockAction.Shutdown,
  warningCount:  Int            = 1000,
  criticalCount: Int            = 5000,
  blockedAfter:  FiniteDuration = 30.seconds,
  growthWindow:  FiniteDuration = 5.minutes,
  growthLimit:   Int            = 200,
  startupGrace:  FiniteDuration = 2.minutes,
  stackDepth:    Int            = 15,
  repeatAfter:   FiniteDuration = 1.minute
)
