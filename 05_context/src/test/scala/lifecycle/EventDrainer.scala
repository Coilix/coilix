package dev.coilix.context
package lifecycle

import dev.coilix.annotation.Component

@Component
final class EventDrainer(journal: Journal) extends ContextLifecycleListener {
  override def closingPhase: ClosingPhase = ClosingPhase.Events

  override def onContextStarted(context: ApplicationContext): Unit = ()

  override def onContextClosing(): Unit = journal.record("events drained")
}
