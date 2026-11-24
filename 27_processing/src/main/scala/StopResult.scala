package dev.coilix.processing

/**
 * The outcome of stopping a processor on this replica.
 *
 * @param id
 *   the identifier of the processor
 * @param stopped
 *   the shards that stopped within the timeout or were not running
 * @param abandoned
 *   the shards whose work did not stop within the timeout and was left behind
 */
final case class StopResult(id: ProcessorId, stopped: List[Int], abandoned: List[Int]) {

  /** Whether every shard stopped within the timeout. */
  def isComplete: Boolean = abandoned.isEmpty
}
