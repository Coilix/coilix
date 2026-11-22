package dev.coilix.events
package listeners

import java.util.concurrent.atomic.AtomicReference

final class Recorder[T] {
  private val values = AtomicReference(List.empty[T])

  def record(value: T): Unit = {
    val _ = values.updateAndGet(_ :+ value)
  }

  def recorded: List[T] = values.get
}
