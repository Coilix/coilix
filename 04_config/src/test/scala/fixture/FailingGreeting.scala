package io.github.coilix.config.fixture

import io.github.coilix.config.ConfigurationException

final class FailingGreeting extends Greeting {
  throw ConfigurationException("boom")

  override def greet(name: String): String = name
}
