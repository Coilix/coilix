package dev.coilix.concurrency

import dev.coilix.config.Environment
import scala.concurrent.duration.DurationInt
import scala.concurrent.duration.FiniteDuration

/**
 * Settings of the reaction to fatal errors, read from `coilix.fatal-errors`. Every key is optional.
 *
 * ```yaml
 * coilix.fatal-errors:
 *   action: shutdown
 *   exit-code: 70
 *   shutdown-timeout: 15s
 * ```
 *
 * @param action
 *   whether a fatal error ends the process or is only logged
 * @param exitCode
 *   the exit code of the process after a fatal error
 * @param shutdownTimeout
 *   how long closing the context may take before the process is halted without running the shutdown hooks
 */
final case class FatalErrorSettings(
  action:          FatalErrorAction = FatalErrorAction.Shutdown,
  exitCode:        Int              = 70,
  shutdownTimeout: FiniteDuration   = 15.seconds
)

/** Reading of [[dev.coilix.concurrency.FatalErrorSettings]] from the configuration. */
object FatalErrorSettings {

  /** The configuration path of the settings. */
  val path: String = "coilix.fatal-errors"

  /**
   * The settings under `coilix.fatal-errors`, with defaults for missing keys.
   *
   * @throws dev.coilix.config.ConfigurationException
   *   if a value cannot be read, such as an unknown action
   */
  def from(environment: Environment): FatalErrorSettings =
    environment.get[FatalErrorSettings](path).getOrElse(FatalErrorSettings())
}
