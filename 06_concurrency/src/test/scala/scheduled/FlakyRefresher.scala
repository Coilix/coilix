package dev.coilix.concurrency
package scheduled

import dev.coilix.annotation.Component
import dev.coilix.annotation.Scheduled
import java.util.concurrent.CountDownLatch
import java.util.concurrent.atomic.AtomicInteger

@Component
final class FlakyRefresher {
  private val runs = AtomicInteger()

  val recovered: CountDownLatch = CountDownLatch(1)

  @Scheduled(fixedDelay = "${refresher.delay:1h}", initialDelay = "5ms")
  def refresh(): Unit =
    if (runs.incrementAndGet() == 1) throw IllegalStateException("expected in test") else recovered.countDown()
}
