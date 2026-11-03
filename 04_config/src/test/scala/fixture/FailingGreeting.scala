package dev.coilix.config.fixture

import dev.coilix.config.ConfigurationException

final class FailingGreeting extends Greeting {
  throw ConfigurationException("boom")

  override def greet(name: String): String = name
}
