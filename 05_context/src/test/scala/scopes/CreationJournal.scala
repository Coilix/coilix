package dev.coilix.context
package scopes

import dev.coilix.annotation.Component
import java.util.concurrent.atomic.AtomicReference

@Component
final class CreationJournal {
  private val entries = AtomicReference(List.empty[String])

  def record(entry: String): Unit = {
    val _ = entries.updateAndGet(_ :+ entry)
  }

  def all: List[String] = entries.get
}
