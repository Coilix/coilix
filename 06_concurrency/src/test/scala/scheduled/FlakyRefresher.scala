package dev.coilix.concurrency
package scheduled

import cats.effect.IO
import dev.coilix.annotation.Component
import dev.coilix.annotation.Scheduled
import java.util.concurrent.CountDownLatch
import java.util.concurrent.atomic.AtomicInteger

@Component
final class FlakyRefresher {
  private val runs = AtomicInteger()

  val recovered: CountDownLatch = CountDownLatch(1)

  @Scheduled(fixedDelay = "${refresher.delay:1h}", initialDelay = "5ms")
  def refresh(): IO[Unit] =
    IO(runs.incrementAndGet()).flatMap(run =>
      if (run == 1) IO.raiseError(IllegalStateException("expected in test")) else IO(recovered.countDown())
    )
}
