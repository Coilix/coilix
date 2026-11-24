package dev.coilix.processing

/** Runs shard `i` on replica `i % replicaCount`, chosen with `assignment = round-robin`, the default. */
final class RoundRobinAssignment extends ShardAssignment {

  override def name: String = "round-robin"

  override def replicasOf(shard: Int, shardCount: Int, replicaCount: Int): List[Int] = List(shard % replicaCount)
}
