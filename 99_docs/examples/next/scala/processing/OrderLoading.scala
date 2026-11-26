package com.example
package processing

import cats.effect.IO
import dev.coilix.processing.Shard
import dev.coilix.processing.StreamProcessor
import dev.coilix.processing.annotation.Processor
import fs2.Pipe
import fs2.Stream
import scala.concurrent.duration.DurationInt

@Processor
final class OrderLoading(feed: OrderFeed, store: OrderStore) extends StreamProcessor[String] {
  override val id: ShopProcessor = ShopProcessor.OrderLoading

  override def source(shard: Shard): Stream[IO, String] = feed.orders

  override def flow(shard: Shard): Pipe[IO, String, Unit] =
    _.groupWithin(50, 5.seconds).evalMap(batch => IO.blocking(store.saveAll(batch.toList)))
}
