package dev.coilix.processing

import dev.coilix.cluster.Member
import java.time.Instant
import munit.FunSuite
import scala.concurrent.duration.DurationInt

final class InMemoryStreamTest extends FunSuite {
  private val replica = Member(0, "replica-0", None, Instant.EPOCH, "test")

  private def shard(index: Int, count: Int = 2, slots: Int = 8): Shard =
    Shard(TestProcessor.Shipping, SlotSet(index, count, slots), replica)

  private def keyed(slot: Int, shard: Shard): Boolean = shard.owns(slot)

  test("a shard reads only the elements of its slots, in the order they were published") {
    val stream   = InMemoryStream[String](8, identity)
    val elements = List("anna", "bob", "carl", "dora", "emil", "fred")
    stream.publish(elements*)
    val first    = stream.source(shard(0)).poll(10, 10.millis)
    val second   = stream.source(shard(1)).poll(10, 10.millis)
    assertEquals((first ++ second).map(_.value).sorted, elements.sorted)
    assert(first.forall(input => keyed(input.slot, shard(0))) && second.forall(input => keyed(input.slot, shard(1))))
    assertEquals(first.map(_.value), elements.filter(first.map(_.value).contains))
    assertEquals(stream.source(shard(0)).slotCount, Some(8))
  }

  test("an acknowledged element leaves the stream and is not delivered again") {
    val stream = InMemoryStream[String](8, identity)
    stream.publish("anna")
    val source = stream.source(shard(0, count = 1))
    source.poll(1, 10.millis).foreach(_.acknowledgement.acknowledge())
    source.close()
    assertEquals((stream.pending, stream.acknowledged), (0, List("anna")))
    assertEquals(stream.source(shard(0, count = 1)).poll(1, 10.millis), Nil)
  }

  test("an element not acknowledged before its source closes comes again with the next delivery number") {
    val stream = InMemoryStream[String](8, identity)
    stream.publish("anna")
    val first  = stream.source(shard(0, count = 1))
    val taken  = first.poll(1, 10.millis)
    assertEquals(stream.source(shard(0, count = 1)).poll(1, 10.millis), Nil)
    first.close()
    val again  = stream.source(shard(0, count = 1)).poll(1, 10.millis)
    assertEquals(again.map(input => (input.value, input.delivery, input.id)), List(("anna", 2, taken.head.id)))
  }

  test("a rejected element comes again and the lag counts the waiting elements of each slot") {
    val stream = InMemoryStream[String](1, identity)
    stream.publish("anna", "bob")
    val source = stream.source(shard(0, count = 1, slots = 1))
    assertEquals(source.lag, Map(0 -> 2L))
    val taken  = source.poll(1, 10.millis)
    assertEquals(source.lag, Map(0 -> 1L))
    taken.foreach(_.acknowledgement.reject())
    assertEquals(source.poll(5, 10.millis).map(input => (input.value, input.delivery)), List(("anna", 2), ("bob", 1)))
  }

  test("a poll waits for elements published later and returns empty after the timeout") {
    val stream    = InMemoryStream[String](1, identity)
    val source    = stream.source(shard(0, count = 1, slots = 1))
    val started   = System.nanoTime
    assertEquals(source.poll(1, 50.millis), Nil)
    assert((System.nanoTime - started) >= 40.millis.toNanos)
    val publisher = Thread(() => {
      Thread.sleep(20)
      stream.publish("anna")
    })
    publisher.start()
    assertEquals(source.poll(1, 5.seconds).map(_.value), List("anna"))
    publisher.join()
  }
}
