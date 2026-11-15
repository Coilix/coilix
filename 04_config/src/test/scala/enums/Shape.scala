package dev.coilix.config
package enums

import dev.coilix.annotation.Enumerated

@Enumerated
enum Shape {
  case Circle(radius: Double)
  case Square(side: Double)
}
