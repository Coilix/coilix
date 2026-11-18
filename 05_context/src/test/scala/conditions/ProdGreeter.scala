package dev.coilix.context
package conditions

import dev.coilix.annotation.Component
import dev.coilix.annotation.Profile

@Component
@Profile(Array("!dev"))
final class ProdGreeter extends Greeter {
  override def greet: String = "prod"
}
