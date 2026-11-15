package dev.coilix.config
package enums

import dev.coilix.annotation.Enumerated

@Enumerated
enum Planet(val mass: Double) {
  case Earth extends Planet(1.0)
  case Mars extends Planet(0.1)
}
