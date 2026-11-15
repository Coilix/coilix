package dev.coilix.context
package lifecycle

import dev.coilix.annotation.Component
import dev.coilix.annotation.PostConstruct

@Component
final class InitWithArgument {

  @PostConstruct
  def init(name: String): Unit = ()
}
