package dev.coilix.diagnostics

import java.util.concurrent.atomic.AtomicReference

/**
 * The `startup` readiness indicator: `Down` while the application starts, `Up` after its runners have finished, and `Down`
 * again when the start failed, so a load balancer sends no traffic to an application that is not ready yet.
 *
 * The [[dev.coilix.diagnostics.StartupHealthListener]] moves it on [[dev.coilix.events.ApplicationStarted]] and
 * [[dev.coilix.events.ApplicationFailed]].
 */
case class StartupHealthIndicator(current: AtomicReference[HealthState]) extends HealthIndicator {

  override def name: String = "startup"

  override def kind: HealthKind = HealthKind.Readiness

  override def state: HealthState = current.get

  /** Marks the start as finished. */
  def started(): Unit = current.set(HealthState.up)

  /** Marks the start as failed with `reason`. */
  def failed(reason: String): Unit = current.set(HealthState.down(s"the application failed to start: $reason"))
}

/** Creation of the [[dev.coilix.diagnostics.StartupHealthIndicator]]. */
object StartupHealthIndicator {

  /** An indicator that is `Down` until the application has started. */
  def apply(): StartupHealthIndicator =
    StartupHealthIndicator(AtomicReference(HealthState.down("the application is starting")))
}
