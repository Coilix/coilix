package dev.coilix.processing

import dev.coilix.cluster.Member

/**
 * The processors of this replica and their shards.
 *
 * @param replica
 *   this replica
 * @param replicaCount
 *   the number of replicas of the application
 * @param processors
 *   every processor, ordered by name
 */
final case class ProcessingTopology(replica: Member, replicaCount: Int, processors: List[ProcessorTopology])
