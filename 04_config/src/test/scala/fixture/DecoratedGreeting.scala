package org.scaling.config.fixture

final class DecoratedGreeting(service: Greeting, suffix: String) extends Greeting {
  override def greet(name: String): String = service.greet(name) + suffix
}
