package dev.coilix.processing

/** The statistics of one processor on this replica: every shard it runs here, with the sums over them. */
final case class ProcessorStatistics(id: ProcessorId, shards: List[ShardStatistics]) {

  /** The elements processed by every shard of this replica. */
  def processed: Long = shards.map(_.metrics.processed).sum

  /** The elements on which the processor failed on this replica. */
  def failed: Long = shards.map(_.metrics.failed).sum

  /** The elements waiting in every slot of the shards of this replica. */
  def lag: Long = shards.map(_.metrics.totalLag).sum

  /** The statistics of shard `index`, if this replica runs it. */
  def shard(index: Int): Option[ShardStatistics] = shards.find(_.index == index)
}
