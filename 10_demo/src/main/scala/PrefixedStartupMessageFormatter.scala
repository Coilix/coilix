package io.github.coilix.demo

import io.github.coilix.boot.StartupMessageFormatter
import scala.concurrent.duration.FiniteDuration

final class PrefixedStartupMessageFormatter(prefix: String, delegate: StartupMessageFormatter) extends StartupMessageFormatter {

  override def starting(applicationName: String): String =
    s"$prefix ${delegate.starting(applicationName)}"

  override def started(applicationName: String, elapsed: FiniteDuration): String =
    s"$prefix ${delegate.started(applicationName, elapsed)}"
}
