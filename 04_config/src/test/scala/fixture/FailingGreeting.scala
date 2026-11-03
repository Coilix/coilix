package dev.coilix.config
package fixture

import dev.coilix.config.ConfigurationException

final class FailingGreeting extends Greeting {
  throw ConfigurationException("boom")

  override def greet(name: String): String = name
}
