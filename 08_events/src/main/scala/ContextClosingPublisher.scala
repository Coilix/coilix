package dev.coilix.events

import cats.effect.IO
import dev.coilix.annotation.Slf4s
import dev.coilix.context.ApplicationContext
import dev.coilix.context.ClosingPhase
import dev.coilix.context.ContextLifecycleListener

/**
 * Publishes [[dev.coilix.events.ContextClosing]] in the [[dev.coilix.context.ClosingPhase.Announcement]] phase, so listeners hear
 * about the closing before `@Scheduled` methods and other sources of work stop. An exception of a listener is logged at `ERROR`
 * level.
 *
 * @param publisher
 *   delivers the event
 * @param context
 *   the context carried by the event
 */
@Slf4s
final class ContextClosingPublisher(publisher: EventPublisher, context: ApplicationContext) extends ContextLifecycleListener {

  override def onContextStarted(context: ApplicationContext): IO[Unit] = IO.unit

  override def closingPhase: ClosingPhase = ClosingPhase.Announcement

  override def onContextClosing: IO[Unit] =
    publisher.publish(ContextClosing(context))
      .handleErrorWith(failure => logF.error("A listener failed on ContextClosing", failure))
}
