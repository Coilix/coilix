package dev.coilix.boot.overrides

import dev.coilix.boot.StartupMessageFormatter
import scala.concurrent.duration.FiniteDuration

final class TaggedStartupMessageFormatter(tag: String, delegate: StartupMessageFormatter) extends StartupMessageFormatter {

  override def starting(applicationName: String): String =
    s"$tag ${delegate.starting(applicationName)}"

  override def started(applicationName: String, elapsed: FiniteDuration): String =
    s"$tag ${delegate.started(applicationName, elapsed)}"
}
