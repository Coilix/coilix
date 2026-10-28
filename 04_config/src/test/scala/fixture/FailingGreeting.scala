package org.scaling.config.fixture

import org.scaling.config.ConfigurationException

final class FailingGreeting extends Greeting {
  throw ConfigurationException("boom")

  override def greet(name: String): String = name
}
