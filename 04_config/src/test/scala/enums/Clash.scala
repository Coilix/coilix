package dev.coilix.config
package enums

import dev.coilix.annotation.EnumName
import dev.coilix.annotation.Enumerated

@Enumerated(ignoreCase = true)
enum Clash {
  @EnumName(value = "a", aliases = Array("B")) case A
  case B
}
