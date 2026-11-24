package dev.coilix.processing

/**
 * Runs every shard on one replica, chosen with `assignment = pinned` and `pinned-replica`.
 *
 * @param replica
 *   the index of the replica that runs the shards
 */
final class PinnedAssignment(val replica: Int) extends ShardAssignment {

  override def name: String = "pinned"

  override def replicasOf(shard: Int, shardCount: Int, replicaCount: Int): List[Int] = List(replica)
}
