package dev.coilix.context
package lifecycle

import cats.effect.IO
import dev.coilix.annotation.Component
import dev.coilix.annotation.PreDestroy

@Component
final class StartWatcher(journal: Journal) extends ContextLifecycleListener {
  override def onContextStarted(context: ApplicationContext): IO[Unit] =
    IO(journal.record(s"started with ${context.beanNames.sorted.mkString(", ")}"))

  override def onContextClosing: IO[Unit] =
    IO(journal.record("closing"))

  @PreDestroy
  def destroy(): Unit = journal.record("watcher destroyed")
}
