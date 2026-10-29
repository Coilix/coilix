package io.github.coilix.boot

import io.github.coilix.config.ComponentProvider
import io.github.coilix.config.ConfigPath
import scala.concurrent.duration.FiniteDuration

trait StartupLogger {
  def starting(applicationName: String): Unit

  def started(applicationName: String, elapsed: FiniteDuration): Unit
}

object StartupLogger {
  given ConfigPath[StartupLogger] = ConfigPath[StartupLogger]("coilix.boot.startup-logger")

  def default(using components: ComponentProvider): StartupLogger =
    components.get[StartupLogger](Slf4jStartupLogger(BannerLoader.default, StartupMessageFormatter.default))
}
