package dev.coilix.processing

/**
 * Runs every shard on every replica, chosen with `assignment = all`.
 *
 * It suits sources that split the work between replicas themselves, such as a group of Kafka consumers.
 */
final class AllReplicasAssignment extends ShardAssignment {

  override def name: String = "all"

  override def replicasOf(shard: Int, shardCount: Int, replicaCount: Int): List[Int] = Range(0, replicaCount).toList
}
