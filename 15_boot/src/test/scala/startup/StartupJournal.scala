package dev.coilix.boot
package startup

import cats.effect.IO
import dev.coilix.annotation.Component
import dev.coilix.annotation.EventListener
import dev.coilix.events.ApplicationFailed
import dev.coilix.events.ApplicationStarted
import dev.coilix.events.ContextReady
import java.util.concurrent.atomic.AtomicReference

@Component
final class StartupJournal {
  private val entries = AtomicReference(List.empty[String])

  def recorded: List[String] = entries.get

  def record(entry: String): Unit = {
    val _ = entries.updateAndGet(_ :+ entry)
  }

  @EventListener
  private def onReady(event: ContextReady): IO[Unit] = IO(record(s"ready:${event.context.beanNames.contains("startupJournal")}"))

  @EventListener
  private def onStarted(event: ApplicationStarted): IO[Unit] = IO(record("started"))

  @EventListener
  private def onFailed(event: ApplicationFailed): IO[Unit] = IO(record(s"failed:${event.failure.getMessage}"))
}
