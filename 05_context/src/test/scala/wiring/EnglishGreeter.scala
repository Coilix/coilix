package org.scaling.context.wiring

import org.scaling.annotation.Component

@Component
final class EnglishGreeter extends Greeter {
  override def greet(name: String): String = s"Hello $name"
}
