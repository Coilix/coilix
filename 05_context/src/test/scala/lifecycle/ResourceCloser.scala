package dev.coilix.context
package lifecycle

import cats.effect.IO
import dev.coilix.annotation.Component

@Component
final class ResourceCloser(journal: Journal) extends ContextLifecycleListener {
  override def closingPhase: ClosingPhase = ClosingPhase.Resources

  override def onContextStarted(context: ApplicationContext): IO[Unit] = IO.unit

  override def onContextClosing: IO[Unit] = IO(journal.record("resources released"))
}
