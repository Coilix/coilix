package com.example
package diagnostics

import dev.coilix.annotation.Component
import java.util.concurrent.atomic.AtomicInteger

@Component
final class StockSync {
  private val pending = AtomicInteger()

  def queue(changes: Int): Unit = {
    val _ = pending.addAndGet(changes)
  }

  def synced(changes: Int): Unit = {
    val _ = pending.addAndGet(-changes)
  }

  def backlog: Int = pending.get
}
