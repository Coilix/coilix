package dev.coilix.processing

/** Runs a contiguous range of shards on each replica, chosen with `assignment = range`. */
final class RangeAssignment extends ShardAssignment {

  override def name: String = "range"

  override def replicasOf(shard: Int, shardCount: Int, replicaCount: Int): List[Int] =
    List((shard.toLong * replicaCount / shardCount).toInt)
}
