package dev.coilix.diagnostics

import cats.effect.IO
import cats.effect.Ref

/**
 * The `startup` readiness indicator: `Down` while the application starts, `Up` after its runners have finished, and `Down`
 * again when the start failed, so a load balancer sends no traffic to an application that is not ready yet.
 *
 * The [[dev.coilix.diagnostics.StartupHealthListener]] moves it on [[dev.coilix.events.ApplicationStarted]] and
 * [[dev.coilix.events.ApplicationFailed]].
 */
case class StartupHealthIndicator(current: Ref[IO, HealthState]) extends HealthIndicator {

  override def name: String = "startup"

  override def kind: HealthKind = HealthKind.Readiness

  override def state: IO[HealthState] = current.get

  /** Marks the start as finished. */
  def started: IO[Unit] = current.set(HealthState.up)

  /** Marks the start as failed with `reason`. */
  def failed(reason: String): IO[Unit] = current.set(HealthState.down(s"the application failed to start: $reason"))
}

/** Creation of the [[dev.coilix.diagnostics.StartupHealthIndicator]]. */
object StartupHealthIndicator {

  /** An indicator that is `Down` until the application has started. */
  def apply(): StartupHealthIndicator =
    StartupHealthIndicator(Ref.unsafe[IO, HealthState](HealthState.down("the application is starting")))
}
