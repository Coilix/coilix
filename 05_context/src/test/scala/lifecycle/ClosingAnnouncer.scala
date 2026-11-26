package dev.coilix.context
package lifecycle

import cats.effect.IO
import dev.coilix.annotation.Component

@Component
final class ClosingAnnouncer(journal: Journal) extends ContextLifecycleListener {
  override def closingPhase: ClosingPhase = ClosingPhase.Announcement

  override def onContextStarted(context: ApplicationContext): IO[Unit] = IO.unit

  override def onContextClosing: IO[Unit] = IO(journal.record("closing announced"))
}
