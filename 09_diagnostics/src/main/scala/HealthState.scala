package dev.coilix.diagnostics

/**
 * The status of a [[dev.coilix.diagnostics.HealthIndicator]] with the reason for it.
 *
 * @param reason
 *   why the indicator is in this status, empty for `Up`
 */
final case class HealthState(status: HealthStatus, reason: String)

/** Factories of [[dev.coilix.diagnostics.HealthState]]. */
object HealthState {

  /** The state of a part that works correctly. */
  val up: HealthState = HealthState(HealthStatus.Up, "")

  /** A `Degraded` state with `reason`. */
  def degraded(reason: String): HealthState = HealthState(HealthStatus.Degraded, reason)

  /** A `Down` state with `reason`. */
  def down(reason: String): HealthState = HealthState(HealthStatus.Down, reason)
}
