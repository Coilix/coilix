package com.example
package tasks

import cats.effect.IO
import dev.coilix.annotation.Component
import dev.coilix.annotation.Scheduled
import java.util.concurrent.atomic.AtomicInteger

@Component
final class PriceRefresher {
  private val counter = AtomicInteger()

  def refreshes: Int = counter.get

  @Scheduled(fixedDelay = "${shop.price-refresh:50ms}", initialDelay = "10ms")
  private def refresh(): IO[Unit] = IO(counter.incrementAndGet()).void
}
