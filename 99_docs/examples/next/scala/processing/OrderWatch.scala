package com.example
package processing

import dev.coilix.processing.BlockingProcessor
import dev.coilix.processing.Shard
import dev.coilix.processing.annotation.Processor
import java.util.concurrent.atomic.AtomicInteger
import scala.annotation.tailrec

@Processor
final class OrderWatch extends BlockingProcessor {
  private val polls = AtomicInteger()

  override val id: ShopProcessor = ShopProcessor.OrderWatch

  def pollCount: Int = polls.get

  @tailrec
  override def run(shard: Shard): Unit =
    if (!shard.stopRequested) {
      polls.incrementAndGet()
      Thread.sleep(100)
      run(shard)
    }
}
