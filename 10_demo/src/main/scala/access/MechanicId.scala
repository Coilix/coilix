package dev.coilix.demo
package access

import dev.coilix.annotation.ValueType

/** Identifier of a mechanic, such as `M-001`. */
@ValueType
final case class MechanicId(value: String) {
  require(value.matches("M-[0-9]{3}"), s"Mechanic identifier must look like M-001, but is '$value'")
}
