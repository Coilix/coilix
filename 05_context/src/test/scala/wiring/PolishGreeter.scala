package io.github.coilix.context.wiring

import io.github.coilix.annotation.Component
import io.github.coilix.annotation.Primary

@Component
@Primary
final class PolishGreeter extends Greeter {
  override def greet(name: String): String = s"Cześć $name"
}
