package io.github.coilix.boot

import io.github.coilix.config.ComponentProvider
import io.github.coilix.config.ConfigPath
import scala.concurrent.duration.FiniteDuration

trait StartupMessageFormatter {
  def starting(applicationName: String): String

  def started(applicationName: String, elapsed: FiniteDuration): String
}

object StartupMessageFormatter {
  given ConfigPath[StartupMessageFormatter] = ConfigPath[StartupMessageFormatter]("coilix.boot.startup-message-formatter")

  def default(using components: ComponentProvider): StartupMessageFormatter =
    components.get[StartupMessageFormatter](DefaultStartupMessageFormatter())
}
