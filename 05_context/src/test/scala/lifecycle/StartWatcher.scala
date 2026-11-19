package dev.coilix.context
package lifecycle

import dev.coilix.annotation.Component
import dev.coilix.annotation.PreDestroy

@Component
final class StartWatcher(journal: Journal) extends ContextLifecycleListener {
  override def onContextStarted(context: ApplicationContext): Unit =
    journal.record(s"started with ${context.beanNames.sorted.mkString(", ")}")

  override def onContextClosing(): Unit =
    journal.record("closing")

  @PreDestroy
  def destroy(): Unit = journal.record("watcher destroyed")
}
