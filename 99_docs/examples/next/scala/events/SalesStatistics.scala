package com.example
package events

import dev.coilix.annotation.Component
import dev.coilix.annotation.EventListener
import java.util.concurrent.atomic.AtomicReference

@Component
final class SalesStatistics {
  private val sales = AtomicReference(BigDecimal(0))

  def total: BigDecimal = sales.get

  @EventListener
  def onOrderPlaced(event: OrderPlaced): Unit = {
    val _ = sales.updateAndGet(_ + event.total)
  }
}
