package dev.coilix.processing

import java.time.Instant

/**
 * The state of one shard on this replica.
 *
 * @param slots
 *   the index of the shard and its slots
 * @param state
 *   the state of its worker
 * @param since
 *   when the worker entered the state
 * @param restarts
 *   how many times the worker was restarted after a failure
 * @param lastFailure
 *   the latest failure, kept after the worker recovers
 */
final case class ShardStatus(slots: SlotSet, state: WorkerState, since: Instant, restarts: Int, lastFailure: Option[FailureInfo]) {

  /** The index of the shard. */
  def index: Int = slots.shard
}
