package dev.coilix.demo

import dev.coilix.annotation.Component
import dev.coilix.annotation.Profile

/** Writes notifications to the console. Used only with the `dev` profile, for example `--coilix.profiles.active=dev`. */
@Component
@Profile(Array("dev"))
final class ConsoleNotifier extends Notifier {
  override def description: String = "console (dev profile)"
}
