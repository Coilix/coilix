package dev.coilix.diagnostics

import cats.effect.IO

/**
 * The health of the application, combined from every [[dev.coilix.diagnostics.HealthIndicator]] of the context.
 *
 * Liveness is the worst status of the `Liveness` indicators, readiness the worst status of all indicators. The values come
 * from the last [[refresh]], which the diagnostics monitor runs every `coilix.diagnostics.interval`, and a change publishes
 * [[dev.coilix.diagnostics.HealthChanged]].
 *
 * ```scala
 * final class StatusPage(health: Health) {
 *   def ready: IO[Boolean] = health.readiness.map(_ == HealthStatus.Up)
 * }
 * ```
 */
trait Health {

  /** Whether the application is alive. */
  def liveness: IO[HealthStatus]

  /** Whether the application should get new work. */
  def readiness: IO[HealthStatus]

  /** The state of every indicator at the last refresh, sorted by name. */
  def indicators: IO[List[IndicatorReport]]

  /** Reads every indicator again and publishes [[dev.coilix.diagnostics.HealthChanged]] for each status that changed. */
  def refresh(): IO[Unit]
}
