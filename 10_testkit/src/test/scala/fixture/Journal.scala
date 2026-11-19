package dev.coilix.testkit
package fixture

import java.util.concurrent.atomic.AtomicReference

final class Journal {

  private val entries = AtomicReference(List.empty[String])

  def record(entry: String): Unit = {
    val _ = entries.updateAndGet(_ :+ entry)
  }

  def recorded: List[String] = entries.get
}
