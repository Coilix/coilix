package dev.coilix.processing

import java.nio.charset.StandardCharsets
import scala.util.hashing.MurmurHash3

/**
 * Stable mapping of keys to slots and of slots to shards.
 *
 * The slot of a key comes from the MurmurHash3 of its bytes, not from `hashCode`, so it stays the same between versions of
 * the JVM, Scala and the application. It does not depend on the number of shards or replicas:
 *
 * ```scala
 * val slot = ShardKeys.slotOf(order.customerId, slotCount = 256)
 * if (shard.slots.contains(slot)) process(order)
 * ```
 */
object ShardKeys {

  /** The slot of a text key, from 0 until `slotCount`. */
  def slotOf(key: String, slotCount: Int): Int = slotOf(key.getBytes(StandardCharsets.UTF_8), slotCount)

  /** The slot of a key given as bytes, from 0 until `slotCount`. */
  def slotOf(key: Array[Byte], slotCount: Int): Int = {
    require(slotCount >= 1, s"slotCount must be at least 1, got $slotCount")
    Math.floorMod(MurmurHash3.bytesHash(key), slotCount)
  }

  /** The shard that owns the slot when the processor has `shardCount` shards. */
  def shardOf(slot: Int, shardCount: Int): Int = {
    require(shardCount >= 1, s"shardCount must be at least 1, got $shardCount")
    Math.floorMod(slot, shardCount)
  }
}
