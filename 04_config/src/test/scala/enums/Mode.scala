package dev.coilix.config
package enums

import dev.coilix.annotation.EnumName
import dev.coilix.annotation.Enumerated

@Enumerated
enum Mode {
  case Fast, Safe
  @EnumName("read-only") case ReadOnly
}
