package dev.coilix.processing

/**
 * The effective settings of one processor, after the configuration, the code and the defaults of Coilix.
 *
 * @param shardCount
 *   the number of shards
 * @param slotCount
 *   the number of slots
 * @param assignment
 *   how shards are assigned to replicas
 * @param enabled
 *   whether the processor starts with the application
 * @param policy
 *   how its shards are supervised
 */
final case class ResolvedProcessorSettings(
  shardCount: Int,
  slotCount:  Int,
  assignment: ShardAssignment,
  enabled:    Boolean,
  policy:     SupervisionPolicy
)
