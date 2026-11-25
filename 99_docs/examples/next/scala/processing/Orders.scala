package com.example
package processing

import dev.coilix.annotation.Repository
import java.util.concurrent.atomic.AtomicInteger

@Repository
final class Orders {
  private val polls = AtomicInteger()

  def poll(): Unit = polls.incrementAndGet(): Unit

  def pollCount: Int = polls.get
}
