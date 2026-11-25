package dev.coilix.processing

/**
 * What a [[dev.coilix.processing.StatelessProcessor]] knows about the element it processes.
 *
 * @param shard
 *   the shard that processes the element, with its [[dev.coilix.processing.ShardMetrics]]
 * @param slot
 *   the slot of the element
 * @param id
 *   the stable identifier of the element
 * @param delivery
 *   how many times the element has been delivered, more than `1` after a restart
 */
final case class ElementContext(shard: Shard, slot: Int, id: InputId, delivery: Int)
