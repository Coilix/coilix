package dev.coilix.demo

import dev.coilix.boot.StartupMessageFormatter
import scala.concurrent.duration.FiniteDuration

/**
 * Replaces the startup message formatter through the configuration, to show how a framework component is replaced.
 *
 * `application.yaml` selects it with a `class` key under `coilix.boot.startup-message-formatter` and passes the other keys
 * to its constructor, including the nested `delegate` component.
 *
 * @param prefix
 *   text added before every message
 * @param delegate
 *   formatter that creates the rest of the message
 */
final class PrefixedStartupMessageFormatter(prefix: String, delegate: StartupMessageFormatter) extends StartupMessageFormatter {

  override def starting(applicationName: String): String =
    s"$prefix ${delegate.starting(applicationName)}"

  override def started(applicationName: String, elapsed: FiniteDuration): String =
    s"$prefix ${delegate.started(applicationName, elapsed)}"
}
