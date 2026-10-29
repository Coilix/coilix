package io.github.coilix.config.fixture

final class PrefixGreeting(prefix: String) extends Greeting {
  override def greet(name: String): String = s"$prefix $name"
}
