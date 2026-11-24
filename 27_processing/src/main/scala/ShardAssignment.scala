package dev.coilix.processing

import dev.coilix.config.ConfigurationException

/**
 * Decides which shards of a processor run on which replica of the application.
 *
 * The assignment is chosen per processor with `coilix.processors.<name>.assignment`, or for every processor with
 * `coilix.processors.defaults.assignment`. When a processor has fewer shards than the application has replicas, some
 * replicas run none of its shards.
 */
trait ShardAssignment {

  /** The name of the assignment in the configuration, such as `round-robin`. */
  def name: String

  /** The replicas, by index, that run the shard. */
  def replicasOf(shard: Int, shardCount: Int, replicaCount: Int): List[Int]

  /** The shards, in ascending order, that run on the replica with the given index. */
  def shardsOf(replica: Int, shardCount: Int, replicaCount: Int): List[Int] =
    Range(0, shardCount).filter(replicasOf(_, shardCount, replicaCount).contains(replica)).toList
}

/** The assignments that need no coordination between replicas. */
object ShardAssignment {

  /** Shard `i` runs on replica `i % replicaCount`. The default. */
  val roundRobin: ShardAssignment = RoundRobinAssignment()

  /** Each replica runs a contiguous range of shards. */
  val range: ShardAssignment = RangeAssignment()

  /** Every shard runs on every replica, for sources that split the work between replicas themselves. */
  val all: ShardAssignment = AllReplicasAssignment()

  /** Every shard runs on one replica. With one shard, the processor runs once in the whole application. */
  def pinned(replica: Int): ShardAssignment = PinnedAssignment(replica)

  /**
   * The assignment with the name from the configuration: `round-robin`, `range`, `all` or `pinned`.
   *
   * @throws dev.coilix.config.ConfigurationException
   *   when the name is unknown
   */
  def named(name: String, pinnedReplica: Int): ShardAssignment =
    name match {
      case "round-robin" => roundRobin
      case "range"       => range
      case "all"         => all
      case "pinned"      => pinned(pinnedReplica)
      case other         =>
        throw ConfigurationException(s"Unknown shard assignment '$other', expected round-robin, range, all or pinned")
    }
}
