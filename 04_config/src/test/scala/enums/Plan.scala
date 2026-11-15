package dev.coilix.config
package enums

import dev.coilix.annotation.EnumName
import dev.coilix.annotation.Enumerated

@Enumerated(ignoreCase = true)
enum Plan {
  @EnumName(value = "basic", aliases = Array("free")) case Basic
  case Premium
}
