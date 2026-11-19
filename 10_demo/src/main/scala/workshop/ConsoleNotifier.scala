package dev.coilix.demo
package workshop

import dev.coilix.annotation.Component
import dev.coilix.annotation.Profile
import dev.coilix.annotation.Slf4s

/** Writes notifications to the log. Used only with the `dev` profile, for example `--coilix.profiles.active=dev`. */
@Component
@Profile(Array("dev"))
@Slf4s
final class ConsoleNotifier extends Notifier {
  override def description: String = "console (dev profile)"

  override def send(message: String): Unit = log.info(s"Notification: $message")
}
