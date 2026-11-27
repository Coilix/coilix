package dev.coilix.concurrency
package scheduled

import cats.effect.IO
import dev.coilix.annotation.Component
import dev.coilix.annotation.Scheduled
import java.util.concurrent.CountDownLatch

@Component
final class Ticker {
  val ticks: CountDownLatch = CountDownLatch(3)

  @Scheduled(fixedRate = "10ms")
  private def tick(): IO[Unit] = IO(ticks.countDown())
}
