package dev.coilix.events
package listeners

import dev.coilix.annotation.Component
import dev.coilix.annotation.PreDestroy
import java.util.concurrent.atomic.AtomicBoolean

@Component
final class MailServer {
  private val open = AtomicBoolean(true)

  def isOpen: Boolean = open.get

  @PreDestroy
  def close(): Unit = open.set(false)
}
