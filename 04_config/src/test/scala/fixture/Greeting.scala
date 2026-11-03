package dev.coilix.config.fixture

import dev.coilix.config.ConfigPath

trait Greeting {
  def greet(name: String): String
}

object Greeting {
  given ConfigPath[Greeting] = ConfigPath[Greeting]("test.greeting")
}
