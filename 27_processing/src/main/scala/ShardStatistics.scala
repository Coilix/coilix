package dev.coilix.processing

/** The state of one shard of this replica with its counters. */
final case class ShardStatistics(status: ShardStatus, metrics: ShardMetricsSnapshot) {

  /** The index of the shard. */
  def index: Int = status.index
}
