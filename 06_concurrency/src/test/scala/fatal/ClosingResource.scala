package dev.coilix.concurrency
package fatal

import dev.coilix.annotation.Component
import dev.coilix.annotation.PreDestroy
import java.util.concurrent.atomic.AtomicBoolean

@Component
final class ClosingResource {
  private val closed = AtomicBoolean(false)

  def isClosed: Boolean = closed.get

  @PreDestroy
  def close(): Unit = closed.set(true)
}
