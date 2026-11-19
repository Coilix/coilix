package dev.coilix.context
package lifecycle

import dev.coilix.annotation.Component

@Component
final class FailingStart(journal: Journal) extends ContextLifecycleListener {
  override def onContextStarted(context: ApplicationContext): Unit =
    throw IllegalStateException("cannot start")

  override def onContextClosing(): Unit =
    throw IllegalStateException("cannot stop")
}
