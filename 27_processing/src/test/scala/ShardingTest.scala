package dev.coilix.processing

import dev.coilix.config.ConfigurationException
import munit.FunSuite

final class ShardingTest extends FunSuite {

  test("names processors in kebab case unless they override the name") {
    assertEquals(
      List(TestProcessor.Ticker, TestProcessor.LegacyName).map(_.name),
      List("ticker", "legacy_flow")
    )
    assertEquals(ProcessorId.kebabCase("VINDecoderSync"), "vin-decoder-sync")
  }

  test("gives each shard the slots whose index modulo the shard count is the shard") {
    val slots = SlotSet(shard = 1, shardCount = 4, slotCount = 10)
    assertEquals((slots.toList, slots.size), (List(1, 5, 9), 3))
    assert(slots.contains(5) && !slots.contains(6) && !slots.contains(13))
  }

  test("maps a key to the same slot whatever the number of shards") {
    val slot = ShardKeys.slotOf("WVWZZZ1JZXW000001", 256)
    assertEquals(ShardKeys.slotOf("WVWZZZ1JZXW000001", 256), slot)
    assert(slot >= 0 && slot < 256)
    assertEquals(List(6, 12).map(count => ShardKeys.shardOf(slot, count)), List(slot % 6, slot % 12))
  }

  test("spreads keys over all slots") {
    val slots = List.range(0, 2000).map(index => ShardKeys.slotOf(s"vehicle-$index", 16)).toSet
    assertEquals(slots, Set.range(0, 16))
  }

  test("gives each shard to exactly one replica with round-robin and range") {
    List(ShardAssignment.roundRobin, ShardAssignment.range).foreach { assignment =>
      val owned = List.range(0, 3).flatMap(replica => assignment.shardsOf(replica, 12, 3))
      assertEquals(owned.sorted, List.range(0, 12), assignment.name)
    }
    assertEquals(ShardAssignment.roundRobin.shardsOf(1, 12, 3), List(1, 4, 7, 10))
    assertEquals(ShardAssignment.range.shardsOf(1, 12, 3), List(4, 5, 6, 7))
  }

  test("leaves replicas without shards when there are fewer shards than replicas") {
    assertEquals(List.range(0, 3).map(ShardAssignment.roundRobin.shardsOf(_, 2, 3)), List(List(0), List(1), Nil))
  }

  test("runs every shard on one replica when pinned and on every replica with all") {
    assertEquals(List.range(0, 3).map(ShardAssignment.pinned(2).shardsOf(_, 1, 3)), List(Nil, Nil, List(0)))
    assertEquals(ShardAssignment.all.replicasOf(1, 2, 3), List(0, 1, 2))
  }

  test("reads assignments by name and rejects unknown ones") {
    assertEquals(
      List("round-robin", "range", "all", "pinned").map(ShardAssignment.named(_, 0).map(_.name)),
      List("round-robin", "range", "all", "pinned").map(Right(_))
    )
    assertEquals(ShardAssignment.named("random", 0).left.map(_.exception.getClass), Left(classOf[ConfigurationException]))
  }
}
