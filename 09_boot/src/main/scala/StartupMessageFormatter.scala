package org.scaling.boot

import org.scaling.config.ComponentProvider
import org.scaling.config.ConfigPath
import scala.concurrent.duration.FiniteDuration

trait StartupMessageFormatter {
  def starting(applicationName: String): String

  def started(applicationName: String, elapsed: FiniteDuration): String
}

object StartupMessageFormatter {
  given ConfigPath[StartupMessageFormatter] = ConfigPath[StartupMessageFormatter]("scaling.boot.startup-message-formatter")

  def default(using components: ComponentProvider): StartupMessageFormatter =
    components.get[StartupMessageFormatter](DefaultStartupMessageFormatter())
}
