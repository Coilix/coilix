package dev.coilix.boot

import dev.coilix.config.ComponentProvider
import dev.coilix.config.ConfigPath
import scala.concurrent.duration.FiniteDuration

/**
 * Formats the messages logged by the default [[dev.coilix.boot.StartupLogger]].
 *
 * Replace it to change the wording, for example to add a prefix:
 *
 * ```yaml
 * coilix:
 *   boot:
 *     startup-message-formatter:
 *       class: com.example.shop.ShopStartupMessageFormatter
 * ```
 */
trait StartupMessageFormatter {

  /** The message logged when the application starts. */
  def starting(applicationName: String): String

  /** The message logged when the context is ready, with the time since the start. */
  def started(applicationName: String, elapsed: FiniteDuration): String
}

/** Configuration path and default implementation of [[dev.coilix.boot.StartupMessageFormatter]]. */
object StartupMessageFormatter {

  /**
   * Replace the component in the configuration under `coilix.boot.startup-message-formatter`, with a `class` key naming the
   * implementation.
   */
  given ConfigPath[StartupMessageFormatter] = ConfigPath[StartupMessageFormatter]("coilix.boot.startup-message-formatter")

  /**
   * The configured implementation, or [[dev.coilix.boot.DefaultStartupMessageFormatter]] when
   * `coilix.boot.startup-message-formatter` is not set.
   */
  def default(using components: ComponentProvider): StartupMessageFormatter =
    components.get[StartupMessageFormatter](DefaultStartupMessageFormatter())
}
