package org.scaling.boot

import org.scaling.config.ComponentProvider
import org.scaling.config.ConfigPath
import scala.concurrent.duration.FiniteDuration

trait StartupLogger {
  def starting(applicationName: String): Unit

  def started(applicationName: String, elapsed: FiniteDuration): Unit
}

object StartupLogger {
  given ConfigPath[StartupLogger] = ConfigPath[StartupLogger]("scaling.boot.startup-logger")

  def default(using components: ComponentProvider): StartupLogger =
    components.get[StartupLogger](Slf4jStartupLogger(BannerLoader.default, StartupMessageFormatter.default))
}
