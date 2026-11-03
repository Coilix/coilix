package dev.coilix.context.wiring

import dev.coilix.annotation.Component
import dev.coilix.annotation.Primary

@Component
@Primary
final class PolishGreeter extends Greeter {
  override def greet(name: String): String = s"Cześć $name"
}
