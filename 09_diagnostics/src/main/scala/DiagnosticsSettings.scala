package dev.coilix.diagnostics

import dev.coilix.config.Environment
import scala.concurrent.duration.DurationInt
import scala.concurrent.duration.FiniteDuration

/**
 * Settings of the diagnostics module, read from `coilix.diagnostics`. Every key is optional.
 *
 * ```yaml
 * coilix.diagnostics:
 *   interval: 5s
 *   threads:
 *     on-deadlock: shutdown
 *     blocked-after: 30s
 * ```
 *
 * @param interval
 *   how often the diagnostics thread checks the threads and refreshes the health
 * @param threads
 *   the settings of the thread checks
 */
final case class DiagnosticsSettings(
  interval: FiniteDuration        = 5.seconds,
  threads:  ThreadMonitorSettings = ThreadMonitorSettings()
)

/** Reading of [[dev.coilix.diagnostics.DiagnosticsSettings]] from the configuration. */
object DiagnosticsSettings {

  /** The configuration path of the settings. */
  val path: String = "coilix.diagnostics"

  /**
   * The settings under `coilix.diagnostics`, with defaults for missing keys.
   *
   * @throws dev.coilix.config.ConfigurationException
   *   if a value cannot be read, such as an unknown `on-deadlock` action
   */
  def from(environment: Environment): DiagnosticsSettings =
    environment.get[DiagnosticsSettings](path).getOrElse(DiagnosticsSettings())
}
