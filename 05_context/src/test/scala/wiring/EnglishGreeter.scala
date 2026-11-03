package dev.coilix.context.wiring

import dev.coilix.annotation.Component

@Component
final class EnglishGreeter extends Greeter {
  override def greet(name: String): String = s"Hello $name"
}
