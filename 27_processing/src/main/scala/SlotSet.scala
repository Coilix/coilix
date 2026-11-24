package dev.coilix.processing

/**
 * The slots that belong to one shard: slot `s` belongs to shard `s % shardCount`.
 *
 * A slot is the smallest fixed part of the data of a processor, such as a partition of a topic or a bucket of key hashes
 * from [[dev.coilix.processing.ShardKeys]]. Slots do not move when the number of shards changes, only the shard that owns
 * them does.
 *
 * @param shard
 *   the index of the shard, from 0 until `shardCount`
 * @param shardCount
 *   the number of shards of the processor
 * @param slotCount
 *   the number of slots of the processor, at least `shardCount`
 */
final case class SlotSet(shard: Int, shardCount: Int, slotCount: Int) {
  require(shardCount >= 1, s"shardCount must be at least 1, got $shardCount")
  require(slotCount >= shardCount, s"slotCount $slotCount must not be less than shardCount $shardCount")
  require(shard >= 0 && shard < shardCount, s"shard must be from 0 until $shardCount, got $shard")

  /** Whether the slot belongs to this shard. */
  def contains(slot: Int): Boolean = slot >= 0 && slot < slotCount && slot % shardCount == shard

  /** The slots of this shard in ascending order. */
  def toList: List[Int] = Range(shard, slotCount, shardCount).toList

  /** The number of slots of this shard. */
  def size: Int = (slotCount - shard + shardCount - 1) / shardCount
}
