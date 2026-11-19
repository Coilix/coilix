package dev.coilix.testkit
package fixture

import dev.coilix.annotation.Component
import dev.coilix.annotation.Profile

@Component
@Profile(Array("!loud"))
final class QuietNotifier extends Notifier {
  override def channel: String = "quiet"
}
