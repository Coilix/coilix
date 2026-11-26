package dev.coilix.context
package lifecycle

import cats.effect.IO
import dev.coilix.annotation.Component

@Component
final class FailingStart(journal: Journal) extends ContextLifecycleListener {
  override def onContextStarted(context: ApplicationContext): IO[Unit] =
    IO.raiseError(IllegalStateException("cannot start"))

  override def onContextClosing: IO[Unit] =
    IO.raiseError(IllegalStateException("cannot stop"))
}
