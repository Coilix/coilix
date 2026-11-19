package dev.coilix.testkit
package fixture

import dev.coilix.annotation.Component

@Component
final class Reception(greeter: Greeter, notifier: Notifier, uplink: Option[Uplink]) {
  def welcome(name: String): String = greeter.greet(name)

  def channel: String = notifier.channel

  def hasUplink: Boolean = uplink.isDefined
}
