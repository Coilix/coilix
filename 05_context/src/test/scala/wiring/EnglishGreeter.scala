package io.github.coilix.context.wiring

import io.github.coilix.annotation.Component

@Component
final class EnglishGreeter extends Greeter {
  override def greet(name: String): String = s"Hello $name"
}
