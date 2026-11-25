package dev.coilix.diagnostics

import java.util.concurrent.atomic.AtomicReference

/**
 * A [[dev.coilix.diagnostics.HealthIndicator]] whose state is set by the code that observes the part, such as the thread
 * monitor for the `threads` indicator. It starts `Up`.
 */
final class SettableHealthIndicator(override val name: String, override val kind: HealthKind) extends HealthIndicator {

  private val current = AtomicReference(HealthState.up)

  override def state: HealthState = current.get

  /** Sets the state returned from now on. */
  def set(state: HealthState): Unit = current.set(state)
}
