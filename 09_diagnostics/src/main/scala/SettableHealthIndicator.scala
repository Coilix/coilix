package dev.coilix.diagnostics

import cats.effect.IO
import cats.effect.Ref

/**
 * A [[dev.coilix.diagnostics.HealthIndicator]] whose state is set by the code that observes the part, such as the thread
 * monitor for the `threads` indicator. It starts `Up`.
 *
 * @param current
 *   the state returned by [[state]]
 */
case class SettableHealthIndicator(name: String, kind: HealthKind, current: Ref[IO, HealthState]) extends HealthIndicator {

  override def state: IO[HealthState] = current.get

  /** Sets the state returned from now on. */
  def set(state: HealthState): IO[Unit] = current.set(state)
}

/** Creation of the [[dev.coilix.diagnostics.SettableHealthIndicator]]. */
object SettableHealthIndicator {

  /** An indicator that is `Up` until its state is set. Creating the `Ref` runs no effect, so the indicator is a plain bean. */
  def apply(name: String, kind: HealthKind): SettableHealthIndicator =
    SettableHealthIndicator(name, kind, Ref.unsafe[IO, HealthState](HealthState.up))
}
