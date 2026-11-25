package dev.coilix.testkit
package effects
package failing

import java.util.concurrent.atomic.AtomicReference

final class FailureJournal {
  private val entries = AtomicReference(List.empty[String])

  def record(entry: String): Unit = {
    val _ = entries.updateAndGet(_ :+ entry)
  }

  def all: List[String] = entries.get
}
