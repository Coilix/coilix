package dev.coilix.context
package effects

import dev.coilix.annotation.PostConstruct

final class LaterCount {

  @PostConstruct
  def count(): Later[Int] = Later(1)
}
