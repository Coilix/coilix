package dev.coilix.config
package valuetype

import dev.coilix.annotation.ValueType

@ValueType
final case class Port(value: Int) {
  require(value > 0, s"Port must be positive, but is $value")
}
