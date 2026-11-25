package com.example
package processing

import cats.effect.IO
import dev.coilix.processing.IOProcessor
import dev.coilix.processing.Shard
import dev.coilix.processing.annotation.Processor
import scala.concurrent.duration.DurationInt

@Processor
final class OrderWatch(orders: Orders) extends IOProcessor {
  override val id: ShopProcessor = ShopProcessor.OrderWatch

  override def run(shard: Shard): IO[Unit] =
    shard.stopRequested.ifM(IO.unit, IO.blocking(orders.poll()) *> IO.sleep(100.millis) *> run(shard))
}
