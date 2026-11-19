package dev.coilix.testkit
package fixture

import dev.coilix.annotation.Component
import dev.coilix.annotation.Value

@Component
final class TemplateGreeter(@Value("${greeting.text}") text: String) extends Greeter {
  override def greet(name: String): String = s"$text, $name"
}
