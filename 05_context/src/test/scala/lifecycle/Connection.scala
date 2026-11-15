package dev.coilix.context
package lifecycle

import dev.coilix.annotation.Component
import dev.coilix.annotation.PostConstruct
import dev.coilix.annotation.PreDestroy
import java.util.concurrent.atomic.AtomicBoolean

@Component
final class Connection(journal: Journal) {
  private val opened = AtomicBoolean(false)

  def isOpen: Boolean = opened.get

  @PostConstruct
  private def open(): Unit = {
    opened.set(true)
    journal.record("connection opened")
  }

  @PreDestroy
  def close(): Unit = journal.record("connection closed")
}
