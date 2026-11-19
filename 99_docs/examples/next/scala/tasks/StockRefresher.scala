package com.example
package tasks

import dev.coilix.annotation.Component
import dev.coilix.concurrency.TaskScheduler
import java.util.concurrent.atomic.AtomicInteger
import scala.concurrent.duration.DurationInt

@Component
final class StockRefresher(scheduler: TaskScheduler) {
  private val counter = AtomicInteger()

  private val refresh = scheduler.scheduleWithFixedDelay(0.seconds, 50.millis) {
    val _ = counter.incrementAndGet()
  }

  def refreshes: Int = counter.get

  def stop(): Unit = refresh.cancel()
}
