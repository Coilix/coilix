package dev.coilix.config
package fixture

final class DecoratedGreeting(service: Greeting, suffix: String) extends Greeting {
  override def greet(name: String): String = service.greet(name) + suffix
}
