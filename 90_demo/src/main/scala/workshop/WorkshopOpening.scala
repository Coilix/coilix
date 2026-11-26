package dev.coilix.demo
package workshop

import cats.effect.IO
import dev.coilix.annotation.Component
import dev.coilix.annotation.Slf4s
import dev.coilix.context.ApplicationContext
import dev.coilix.context.ClosingPhase
import dev.coilix.context.ContextLifecycleListener
import java.util.concurrent.atomic.AtomicBoolean

/**
 * Opens the workshop when every bean exists and closes it before any bean is destroyed.
 *
 * The workshop stays open in the [[dev.coilix.context.ClosingPhase.Resources]] phase, so it closes only after the sources of
 * new work, such as the [[dev.coilix.demo.access.ClaimSweeper]], have stopped and the accepted work is finished.
 */
@Component
@Slf4s
final class WorkshopOpening(properties: WorkshopProperties) extends ContextLifecycleListener {
  private val open = AtomicBoolean(false)

  /** Whether the workshop is open. */
  def isOpen: Boolean = open.get

  override def onContextStarted(context: ApplicationContext): IO[Unit] =
    IO(open.set(true)) *> logF.info(s"${properties.name} is open with ${context.beanNames.size} beans")

  override def closingPhase: ClosingPhase = ClosingPhase.Resources

  override def onContextClosing: IO[Unit] =
    IO(open.set(false)) *> logF.info(s"${properties.name} is closing")
}
