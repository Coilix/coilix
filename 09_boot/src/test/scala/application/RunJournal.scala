package org.scaling.boot.application

import java.util.concurrent.atomic.AtomicReference
import org.scaling.annotation.Component

@Component
final class RunJournal {

  private val entries = AtomicReference(List.empty[String])

  def record(entry: String): Unit = {
    val _ = entries.updateAndGet(_ :+ entry)
  }

  def recorded: List[String] = entries.get
}
