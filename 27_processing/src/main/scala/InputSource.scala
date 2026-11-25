package dev.coilix.processing

import scala.concurrent.duration.FiniteDuration

/**
 * Where the elements of a [[dev.coilix.processing.StatelessProcessor]] come from, such as a topic, a queue or a folder.
 *
 * A source is created for one shard and reads only the slots of that shard. The worker of the shard calls [[poll]] in a loop
 * on its own thread, so a connector may block in it and needs no stream library, and closes the source when the shard stops
 * or the worker restarts. An element that was polled but not acknowledged before the source closed must be delivered again.
 *
 * ```scala
 * override def source(shard: Shard): InputSource[Order] = orders.source(shard)
 * ```
 */
trait InputSource[T] extends AutoCloseable {

  /**
   * The number of slots the source itself defines, such as the partitions of a topic, or empty when it follows the slots of
   * the processor. When it is defined, the `slots` of the processor must be the same, otherwise the worker fails at start.
   */
  def slotCount: Option[Int] = None

  /**
   * Up to `max` elements of the slots of the shard, waiting at most `timeout` for the first one; empty when none came.
   *
   * @throws java.lang.InterruptedException
   *   if the worker thread is interrupted while it waits, as it is when the shard stops
   */
  def poll(max: Int, timeout: FiniteDuration): List[Input[T]]

  /** How many elements wait in each slot of the shard, by slot, for the statistics; empty when the source cannot tell. */
  def lag: Map[Int, Long] = Map.empty

  /** Stops fetching new elements without giving up the slots. By default it does nothing. */
  def pause(): Unit = ()

  /** Fetches elements again after [[pause]]. By default it does nothing. */
  def resume(): Unit = ()

  /** Releases the connections and gives back the elements that were polled but not acknowledged. By default it does nothing. */
  override def close(): Unit = ()
}
