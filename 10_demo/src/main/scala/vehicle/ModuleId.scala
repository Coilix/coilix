package dev.coilix.demo
package vehicle

import dev.coilix.annotation.ValueType

/** Identifier of a control module in a vehicle, such as `engine` or `abs`. */
@ValueType
final case class ModuleId(value: String) {
  require(value.matches("[a-z][a-z0-9-]*"), s"Module identifier must be lower case, but is '$value'")
}
