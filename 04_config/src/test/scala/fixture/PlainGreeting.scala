package org.scaling.config.fixture

final class PlainGreeting extends Greeting {
  override def greet(name: String): String = s"Hi $name"
}
