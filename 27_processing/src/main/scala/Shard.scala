package dev.coilix.processing

import dev.coilix.cluster.Member
import java.time.Clock
import java.util.concurrent.atomic.AtomicBoolean

/**
 * One shard of a processor that runs on this replica: its index, its slots and the replica that runs it.
 *
 * The manager gives a new `Shard` to every start of the shard's worker. A processor reads
 * [[dev.coilix.processing.Shard.stopRequested]] to end its work when the shard stops:
 *
 * ```scala
 * override def run(shard: Shard): Unit =
 *   if (!shard.stopRequested) {
 *     exportNext(shard.slots)
 *     run(shard)
 *   }
 * ```
 *
 * @param processor
 *   the identifier of the processor
 * @param slots
 *   the index of the shard, the number of shards and the slots of the shard
 * @param replica
 *   the replica that runs the shard
 * @param metrics
 *   the counters of the shard, the same object for every start of its worker
 */
final class Shard(
  val processor: ProcessorId,
  val slots:     SlotSet,
  val replica:   Member,
  val metrics:   ShardMetrics = ShardMetrics(Clock.systemUTC)
) {

  private val stopping = AtomicBoolean(false)

  /** The index of the shard, from 0 until [[dev.coilix.processing.Shard.count]]. */
  def index: Int = slots.shard

  /** The number of shards of the processor. */
  def count: Int = slots.shardCount

  /** Whether the slot belongs to this shard. */
  def owns(slot: Int): Boolean = slots.contains(slot)

  /** Whether the shard is stopping, so the processor should finish its current work and return. */
  def stopRequested: Boolean = stopping.get

  /** Asks the processor to stop its work on this shard. */
  def requestStop(): Unit = stopping.set(true)

  /** The name of the processor with the index of the shard, such as `invoice-export#2`. */
  override def toString: String = s"${processor.name}#$index"
}
