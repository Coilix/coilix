package org.scaling.config.fixture

import org.scaling.config.ConfigPath

trait Greeting {
  def greet(name: String): String
}

object Greeting {
  given ConfigPath[Greeting] = ConfigPath[Greeting]("test.greeting")
}
