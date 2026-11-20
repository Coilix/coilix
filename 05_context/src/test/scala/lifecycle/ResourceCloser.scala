package dev.coilix.context
package lifecycle

import dev.coilix.annotation.Component

@Component
final class ResourceCloser(journal: Journal) extends ContextLifecycleListener {
  override def closingPhase: ClosingPhase = ClosingPhase.Resources

  override def onContextStarted(context: ApplicationContext): Unit = ()

  override def onContextClosing(): Unit = journal.record("resources released")
}
