package dev.coilix.context
package effects

import dev.coilix.annotation.PostConstruct
import dev.coilix.annotation.PreDestroy
import java.util.concurrent.atomic.AtomicReference

final class LaterWarmup {

  private val entries = AtomicReference(List.empty[String])

  def journal: List[String] = entries.get

  @PostConstruct
  def prepare(): Unit = record("prepare")

  @PostConstruct
  def warm(): Later[Unit] = {
    record("warm described")
    Later(())
  }

  @PreDestroy
  def cool(): Later[Unit] = Later(())

  @PreDestroy
  def stop(): Unit = record("stop")

  private def record(entry: String): Unit = {
    val _ = entries.updateAndGet(_ :+ entry)
  }
}
