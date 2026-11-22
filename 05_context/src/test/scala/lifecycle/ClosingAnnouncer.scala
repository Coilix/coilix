package dev.coilix.context
package lifecycle

import dev.coilix.annotation.Component

@Component
final class ClosingAnnouncer(journal: Journal) extends ContextLifecycleListener {
  override def closingPhase: ClosingPhase = ClosingPhase.Announcement

  override def onContextStarted(context: ApplicationContext): Unit = ()

  override def onContextClosing(): Unit = journal.record("closing announced")
}
