package dev.coilix.processing

import java.util.concurrent.atomic.AtomicLong
import java.util.concurrent.atomic.AtomicReference
import scala.annotation.tailrec
import scala.concurrent.duration.Deadline
import scala.concurrent.duration.FiniteDuration

/**
 * A stream of elements in memory with slots, for tests, demos and work that may be lost when the process ends.
 *
 * [[publish]] puts each element into the slot of its key ([[dev.coilix.processing.ShardKeys.slotOf]]), so elements with one
 * key keep their order. [[source]] gives a shard an [[dev.coilix.processing.InputSource]] that reads only the slots of that
 * shard. An acknowledged element leaves the stream; an element that was polled but not acknowledged when its source closed, or
 * that was rejected, is delivered again with a higher delivery number.
 *
 * ```scala
 * @Component
 * final class OrderStream extends InMemoryStream[Order](256, _.customer)
 * ```
 *
 * @param slotCount
 *   the number of slots, which must equal the `slots` of the processor that reads the stream (256 by default)
 * @param keyOf
 *   the key that chooses the slot of an element
 */
class InMemoryStream[T](slotCount: Int, keyOf: T => String) {

  private val lock     = new AnyRef
  private val entries  = AtomicReference(Vector.empty[InMemoryStream.Entry[T]])
  private val done     = AtomicReference(Vector.empty[T])
  private val sequence = AtomicLong()

  /** Adds `elements` at the end of their slots and wakes up the sources that wait. */
  def publish(elements: T*): Unit =
    lock.synchronized {
      val added = elements.map(element => InMemoryStream.Entry(sequence.incrementAndGet(), slotOf(element), element, 0, None))
      entries.set(entries.get ++ added)
      lock.notifyAll()
    }

  /** A source that reads the slots of `shard`. */
  def source(shard: Shard): InputSource[T] = Source(shard, sequence.incrementAndGet())

  /** The number of elements that are not acknowledged yet, including those being processed. */
  def pending: Int = entries.get.size

  /** The acknowledged elements, in the order they were acknowledged. */
  def acknowledged: List[T] = done.get.toList

  private def slotOf(element: T): Int = ShardKeys.slotOf(keyOf(element), slotCount)

  private def take(shard: Shard, holder: Long, max: Int): List[Input[T]] =
    lock.synchronized {
      val free  = entries.get.filter(entry => entry.holder.isEmpty && shard.owns(entry.slot)).take(max)
      val taken = free.map(entry => entry.id -> entry.copy(deliveries = entry.deliveries + 1, holder = Some(holder))).toMap
      entries.set(entries.get.map(entry => taken.getOrElse(entry.id, entry)))
      free.toList.map(entry => inputOf(taken(entry.id)))
    }

  private def inputOf(entry: InMemoryStream.Entry[T]): Input[T] =
    Input(
      value           = entry.value,
      slot            = entry.slot,
      id              = InputId(s"in-memory-${entry.id}"),
      delivery        = entry.deliveries,
      acknowledgement = acknowledgementOf(entry.id)
    )

  private def acknowledgementOf(id: Long): Acknowledgement =
    new Acknowledgement {
      override def acknowledge(): Unit = remove(id)

      override def reject(): Unit = release(_.id == id)
    }

  private def remove(id: Long): Unit =
    lock.synchronized {
      entries.get.find(_.id == id).foreach(entry => done.set(done.get :+ entry.value))
      entries.set(entries.get.filterNot(_.id == id))
    }

  private def release(which: InMemoryStream.Entry[T] => Boolean): Unit =
    lock.synchronized {
      entries.set(entries.get.map(entry => if (which(entry)) entry.copy(holder = None) else entry))
      lock.notifyAll()
    }

  private def lagOf(shard: Shard): Map[Int, Long] =
    entries.get.filter(entry => entry.holder.isEmpty && shard.owns(entry.slot)).groupMapReduce(_.slot)(_ => 1L)(_ + _)

  private final class Source(shard: Shard, holder: Long) extends InputSource[T] {

    override def slotCount: Option[Int] = Some(InMemoryStream.this.slotCount)

    override def poll(max: Int, timeout: FiniteDuration): List[Input[T]] = waitFor(max, timeout.fromNow)

    override def lag: Map[Int, Long] = lagOf(shard)

    override def close(): Unit = release(_.holder.contains(holder))

    @tailrec
    private def waitFor(max: Int, deadline: Deadline): List[Input[T]] =
      take(shard, holder, max) match {
        case Nil if deadline.hasTimeLeft() =>
          lock.synchronized(lock.wait(math.max(deadline.timeLeft.toMillis, 1L)))
          waitFor(max, deadline)
        case inputs                        => inputs
      }
  }
}

/** The entries of an [[dev.coilix.processing.InMemoryStream]]. */
object InMemoryStream {

  /** One element in the stream with the source that holds it, if any. */
  private final case class Entry[T](id: Long, slot: Int, value: T, deliveries: Int, holder: Option[Long])
}
