package dev.coilix.demo
package vehicle

import dev.coilix.annotation.ValueType

/**
 * Vehicle identification number: 17 digits and capital letters without `I`, `O` and `Q`.
 *
 * A [[dev.coilix.annotation.ValueType]], so the configuration writes it as a plain string, and Coilix rejects an invalid
 * number when it binds the configuration.
 */
@ValueType
final case class Vin(value: String) {
  require(value.matches("[A-HJ-NPR-Z0-9]{17}"), s"VIN must have 17 characters without I, O and Q, but is '$value'")
}
