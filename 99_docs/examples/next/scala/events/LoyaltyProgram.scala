package com.example
package events

import cats.effect.IO
import dev.coilix.annotation.Component
import dev.coilix.events.ApplicationEventListener
import java.util.concurrent.atomic.AtomicReference

@Component
final class LoyaltyProgram extends ApplicationEventListener[OrderPlaced] {
  private val points = AtomicReference(Map.empty[String, Int])

  def pointsOf(customer: String): Int = points.get.getOrElse(customer, 0)

  override def onEvent(event: OrderPlaced): IO[Unit] = {
    val earned = event.total.toInt / 10
    IO(points.updateAndGet(all => all.updated(event.customer, all.getOrElse(event.customer, 0) + earned))).void
  }
}
