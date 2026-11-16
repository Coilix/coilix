package com.example

import dev.coilix.annotation.ValueType

@ValueType
final case class Port(value: Int) {
  require(value > 0 && value < 65536, s"Port must be between 1 and 65535, but is $value")
}
