package dev.coilix.processing

import dev.coilix.common.CoilixException

/**
 * Thrown by a local operation of the [[dev.coilix.processing.ProcessorManager]] on a shard that this replica does not
 * run.
 *
 * @param owners
 *   the replicas, by index, that run the shard, empty when the processor has no such shard
 */
final class ShardNotOwnedException(val id: ProcessorId, val shard: Int, val owners: List[Int])
    extends CoilixException(ShardNotOwnedException.message(id, shard, owners))

private object ShardNotOwnedException {

  def message(id: ProcessorId, shard: Int, owners: List[Int]): String =
    if (owners.isEmpty) s"Processor ${id.name} has no shard $shard"
    else s"Shard $shard of processor ${id.name} runs on replica ${owners.mkString(", ")}, not on this one"
}
