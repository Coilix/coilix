package dev.coilix.concurrency
package fatal

import dev.coilix.annotation.Component
import dev.coilix.annotation.PreDestroy

@Component
final class SlowClosingResource {

  @PreDestroy
  def close(): Unit = Thread.sleep(2000)
}
