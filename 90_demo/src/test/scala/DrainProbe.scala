package dev.coilix.demo

import cats.effect.IO
import cats.syntax.all.*
import dev.coilix.context.ApplicationContext
import dev.coilix.context.ClosingPhase
import dev.coilix.context.ContextLifecycleListener
import dev.coilix.demo.access.ClaimSweeper
import dev.coilix.demo.workshop.WorkshopOpening
import java.util.concurrent.atomic.AtomicReference

final class DrainProbe extends ContextLifecycleListener {
  private val context  = AtomicReference(Option.empty[ApplicationContext])
  private val observed = AtomicReference(Option.empty[(Boolean, Boolean)])

  def sweepingAndOpenWhileDraining: Option[(Boolean, Boolean)] = observed.get

  override def closingPhase: ClosingPhase = ClosingPhase.Events

  override def onContextStarted(started: ApplicationContext): IO[Unit] = IO(context.set(Some(started)))

  override def onContextClosing: IO[Unit] =
    context.get.traverse(beans =>
      beans.getBean[ClaimSweeper].isSweeping.map(sweeping => (sweeping, beans.getBean[WorkshopOpening].isOpen))
    )
      .flatMap(seen => IO(observed.set(seen)))
}
