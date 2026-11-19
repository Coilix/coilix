package dev.coilix.testkit
package fixture

import dev.coilix.annotation.Component
import dev.coilix.annotation.Profile

@Component
@Profile(Array("loud"))
final class LoudNotifier extends Notifier {
  override def channel: String = "loud"
}
