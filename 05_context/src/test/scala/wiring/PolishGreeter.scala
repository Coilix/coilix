package org.scaling.context.wiring

import org.scaling.annotation.Component
import org.scaling.annotation.Primary

@Component
@Primary
final class PolishGreeter extends Greeter {
  override def greet(name: String): String = s"Cześć $name"
}
