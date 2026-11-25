package dev.coilix.processing

import java.time.Instant
import scala.concurrent.duration.Duration
import scala.concurrent.duration.FiniteDuration

/**
 * The counters of a shard at one moment.
 *
 * @param processed
 *   the elements processed since the start of the application, across restarts
 * @param failed
 *   the elements on which the processor failed
 * @param totalTime
 *   the time spent processing the `processed` elements
 * @param longestTime
 *   the longest time one element took
 * @param lastProcessed
 *   when the last element was processed
 * @param lag
 *   how many elements wait in each slot of the shard, as the source last told
 */
final case class ShardMetricsSnapshot(
  processed:     Long,
  failed:        Long,
  totalTime:     FiniteDuration,
  longestTime:   FiniteDuration,
  lastProcessed: Option[Instant],
  lag:           Map[Int, Long]
) {

  /** The average time of one processed element, zero before the first one. */
  def averageTime: FiniteDuration = if (processed == 0) Duration.Zero else totalTime / processed

  /** The elements waiting in all slots of the shard. */
  def totalLag: Long = lag.values.sum
}
