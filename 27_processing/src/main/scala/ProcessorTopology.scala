package dev.coilix.processing

/**
 * How a processor is split and which of its shards run on this replica.
 *
 * @param id
 *   the identifier of the processor
 * @param slotCount
 *   the number of slots
 * @param shardCount
 *   the number of shards in the whole application
 * @param assignment
 *   the name of the assignment of shards to replicas
 * @param shards
 *   the shards of this replica with their slots, empty when this replica runs none
 */
final case class ProcessorTopology(id: ProcessorId, slotCount: Int, shardCount: Int, assignment: String, shards: List[SlotSet])
