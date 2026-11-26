package dev.coilix.processing

import cats.effect.IO
import fs2.Pipe
import fs2.Stream

/**
 * A processor whose work on a shard is an fs2 flow: a [[dev.coilix.processing.StreamProcessor.source]] of elements and a
 * [[dev.coilix.processing.StreamProcessor.flow]] that processes them, run on its own fiber.
 *
 * Coilix puts the stop signal of the shard right after the source and reads the source on its own fiber, one chunk ahead of
 * the flow. Stopping the shard ends the source, so it takes nothing new, while the elements already taken, also an element
 * the flow is processing at that moment, go on through the flow, which then ends like on the end of the source: batches are
 * emitted, writes and commits finish. The flow has `drain-timeout` of the processor to end, by default
 * half of the stop timeout, and is cancelled after it. When the source ends by itself, the work is complete and
 * `on-completion` decides what happens next; a failure restarts it under the restart policy:
 *
 * ```scala
 * @Processor
 * final class OrderLoading(feed: OrderFeed, orders: OrderRepository) extends StreamProcessor[Order] {
 *   override val id: ShopProcessor = ShopProcessor.OrderLoading
 *
 *   override def source(shard: Shard): Stream[IO, Order] = feed.orders(shard.slots)
 *
 *   override def flow(shard: Shard): Pipe[IO, Order, Unit] =
 *     _.groupWithin(50, 5.seconds).evalMap(batch => orders.saveAll(batch.toList))
 * }
 * ```
 *
 * @tparam A
 *   the type of the elements of the source
 */
trait StreamProcessor[A] extends ProcessorDefinition {

  /** The elements of the shard, read until the source ends or the shard stops. */
  def source(shard: Shard): Stream[IO, A]

  /** Processes the elements of the source; its output is discarded, so an override may declare any output type. */
  def flow(shard: Shard): Pipe[IO, A, Any]

  /**
   * The whole work on the shard: the source, ended by the stop signal of the shard, through the flow. The source runs apart
   * from the flow, so the stop signal interrupts only the reading, never a step of the flow.
   */
  final def run(shard: Shard): IO[Unit] =
    source(shard).interruptWhen(shard.stopped.attempt).prefetch.through(flow(shard)).compile.drain
}
