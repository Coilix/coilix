package dev.coilix.processing

import java.time.Clock
import java.time.Instant
import java.util.concurrent.atomic.AtomicLong
import java.util.concurrent.atomic.AtomicReference
import scala.concurrent.duration.DurationLong
import scala.concurrent.duration.FiniteDuration

/**
 * The counters of one shard, kept across restarts of its worker and read through
 * [[dev.coilix.processing.ProcessorManager.statistics]]. The workers of the built-in styles record into it themselves, and a
 * [[dev.coilix.processing.CustomProcessor]] or [[dev.coilix.processing.BlockingProcessor]] records through `shard.metrics`.
 * Every method is safe to call from many threads.
 */
final class ShardMetrics(clock: Clock) {

  private val processed     = AtomicLong()
  private val failed        = AtomicLong()
  private val totalNanos    = AtomicLong()
  private val longestNanos  = AtomicLong()
  private val lastProcessed = AtomicReference(Option.empty[Instant])
  private val lagBySlot     = AtomicReference(Map.empty[Int, Long])

  /** Counts one processed element that took `duration`. */
  def recordProcessed(duration: FiniteDuration): Unit = {
    val nanos = duration.toNanos
    val _     = processed.incrementAndGet()
    val _     = totalNanos.addAndGet(nanos)
    val _     = longestNanos.accumulateAndGet(nanos, math.max)
    lastProcessed.set(Some(clock.instant))
  }

  /** Counts one element that failed. */
  def recordFailed(): Unit = {
    val _ = failed.incrementAndGet()
  }

  /** Sets how many elements wait in each slot, by slot. */
  def recordLag(lag: Map[Int, Long]): Unit = lagBySlot.set(lag)

  /** The counters now. */
  def snapshot: ShardMetricsSnapshot =
    ShardMetricsSnapshot(
      processed.get,
      failed.get,
      totalNanos.get.nanos,
      longestNanos.get.nanos,
      lastProcessed.get,
      lagBySlot.get
    )
}
