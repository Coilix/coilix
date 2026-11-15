package dev.coilix.context
package lifecycle

import dev.coilix.annotation.Component
import dev.coilix.annotation.PostConstruct

@Component
final class FailingInit {

  @PostConstruct
  def init(): Unit = throw IllegalStateException("no connection")
}
