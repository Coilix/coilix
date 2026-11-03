package dev.coilix.boot

import dev.coilix.config.ComponentProvider
import dev.coilix.config.ConfigPath
import scala.concurrent.duration.FiniteDuration

/** Reports the start of the application and how long it took. */
trait StartupLogger {

  /** Called before the components are scanned. */
  def starting(applicationName: String): Unit

  /** Called after the context is ready and before the runners run. */
  def started(applicationName: String, elapsed: FiniteDuration): Unit
}

/** Configuration path and default implementation of [[dev.coilix.boot.StartupLogger]]. */
object StartupLogger {

  /**
   * Replace the component in the configuration under `coilix.boot.startup-logger`, with a `class` key naming the implementation.
   */
  given ConfigPath[StartupLogger] = ConfigPath[StartupLogger]("coilix.boot.startup-logger")

  /** The configured implementation, or [[dev.coilix.boot.Slf4jStartupLogger]] when `coilix.boot.startup-logger` is not set. */
  def default(using components: ComponentProvider): StartupLogger =
    components.get[StartupLogger](Slf4jStartupLogger(BannerLoader.default, StartupMessageFormatter.default))
}
