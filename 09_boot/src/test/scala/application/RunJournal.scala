package io.github.coilix.boot.application

import io.github.coilix.annotation.Component
import java.util.concurrent.atomic.AtomicReference

@Component
final class RunJournal {

  private val entries = AtomicReference(List.empty[String])

  def record(entry: String): Unit = {
    val _ = entries.updateAndGet(_ :+ entry)
  }

  def recorded: List[String] = entries.get
}
