package dev.coilix.processing

/**
 * The state of a processor on this replica.
 *
 * @param id
 *   the identifier of the processor
 * @param shards
 *   the shards that this replica runs, by index
 */
final case class ProcessorStatus(id: ProcessorId, shards: List[ShardStatus]) {

  /** The state of the shard with the given index, if this replica runs it. */
  def shard(index: Int): Option[ShardStatus] = shards.find(_.index == index)

  /** Whether every shard of this replica is in the given state. */
  def all(state: WorkerState): Boolean = shards.forall(_.state == state)
}
