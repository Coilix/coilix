package dev.coilix.processing

import java.util.concurrent.atomic.AtomicReference

/** An [[dev.coilix.processing.Output]] that keeps every emitted element in memory, for tests and demos. */
class InMemoryOutput[R] extends Output[R] {

  private val kept = AtomicReference(Vector.empty[R])

  override def emit(elements: List[R]): Unit = {
    val _ = kept.updateAndGet(_ ++ elements)
  }

  /** Every element emitted so far, in order. */
  def emitted: List[R] = kept.get.toList
}
