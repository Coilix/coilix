package dev.coilix.processing

import java.util.concurrent.atomic.AtomicReference
import scala.annotation.tailrec
import scala.concurrent.duration.Deadline
import scala.concurrent.duration.DurationInt
import scala.concurrent.duration.FiniteDuration

/** What the processors of a test did, in order, safe to write from many threads. */
final class Journal {

  private val entries = AtomicReference(Vector.empty[String])

  def add(entry: String): Unit = entries.getAndUpdate(_ :+ entry): Unit

  def all: Vector[String] = entries.get

  def contains(entry: String): Boolean = all.contains(entry)

  def count(entry: String): Int = all.count(_ == entry)

  def startingWith(prefix: String): Vector[String] = all.filter(_.startsWith(prefix))
}

object Journal {

  /** Waits until the condition holds, at most `timeout`, and tells whether it held. */
  def waitUntil(condition: => Boolean, timeout: FiniteDuration = 5.seconds): Boolean = waitFor(condition, timeout.fromNow)

  @tailrec
  private def waitFor(condition: => Boolean, deadline: Deadline): Boolean =
    if (condition) true
    else if (deadline.isOverdue()) false
    else {
      Thread.sleep(5)
      waitFor(condition, deadline)
    }
}
