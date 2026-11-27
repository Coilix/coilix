package dev.coilix.concurrency
package scheduled

import cats.effect.IO
import dev.coilix.annotation.Component
import dev.coilix.context.ApplicationContext
import dev.coilix.context.ClosingPhase
import dev.coilix.context.ContextLifecycleListener
import java.util.concurrent.atomic.AtomicReference

@Component
final class EventDrainWatcher(registrar: ScheduledMethodRegistrar) extends ContextLifecycleListener {
  private val tasksAtDrain = AtomicReference(Option.empty[Int])

  def activeTasksWhenDraining: Option[Int] = tasksAtDrain.get

  override def closingPhase: ClosingPhase = ClosingPhase.Events

  override def onContextStarted(context: ApplicationContext): IO[Unit] = IO.unit

  override def onContextClosing: IO[Unit] = registrar.activeTasks.flatMap(active => IO(tasksAtDrain.set(Some(active))))
}
