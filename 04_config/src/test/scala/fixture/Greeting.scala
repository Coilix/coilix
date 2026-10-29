package io.github.coilix.config.fixture

import io.github.coilix.config.ConfigPath

trait Greeting {
  def greet(name: String): String
}

object Greeting {
  given ConfigPath[Greeting] = ConfigPath[Greeting]("test.greeting")
}
