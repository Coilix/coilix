package dev.coilix.diagnostics

/**
 * The health of the application, combined from every [[dev.coilix.diagnostics.HealthIndicator]] of the context.
 *
 * Liveness is the worst status of the `Liveness` indicators, readiness the worst status of all indicators. The values come
 * from the last [[refresh]], which the diagnostics thread runs every `coilix.diagnostics.interval`, and a change publishes
 * [[dev.coilix.diagnostics.HealthChanged]].
 *
 * ```scala
 * final class StatusPage(health: Health) {
 *   def ready: Boolean = health.readiness == HealthStatus.Up
 * }
 * ```
 */
trait Health {

  /** Whether the application is alive. */
  def liveness: HealthStatus

  /** Whether the application should get new work. */
  def readiness: HealthStatus

  /** The state of every indicator at the last refresh, sorted by name. */
  def indicators: List[IndicatorReport]

  /** Reads every indicator again and publishes [[dev.coilix.diagnostics.HealthChanged]] for each status that changed. */
  def refresh(): Unit
}
