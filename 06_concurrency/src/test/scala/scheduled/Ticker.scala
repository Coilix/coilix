package dev.coilix.concurrency
package scheduled

import dev.coilix.annotation.Component
import dev.coilix.annotation.Scheduled
import java.util.concurrent.CountDownLatch
import java.util.concurrent.atomic.AtomicReference

@Component
final class Ticker {
  val ticks: CountDownLatch = CountDownLatch(3)

  val thread: AtomicReference[String] = AtomicReference("")

  @Scheduled(fixedRate = "10ms")
  private def tick(): Unit = {
    thread.set(Thread.currentThread.getName)
    ticks.countDown()
  }
}
