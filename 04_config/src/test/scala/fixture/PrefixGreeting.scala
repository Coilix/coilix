package dev.coilix.config
package fixture

final class PrefixGreeting(prefix: String) extends Greeting {
  override def greet(name: String): String = s"$prefix $name"
}
