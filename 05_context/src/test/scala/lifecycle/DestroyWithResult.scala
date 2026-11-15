package dev.coilix.context
package lifecycle

import dev.coilix.annotation.Component
import dev.coilix.annotation.PreDestroy

@Component
final class DestroyWithResult {

  @PreDestroy
  def shutdown(): Int = 0
}
