package dev.coilix.concurrency

import cats.effect.Deferred
import cats.effect.FiberIO
import cats.effect.IO
import cats.effect.Ref
import cats.effect.Unique
import cats.syntax.all.*
import dev.coilix.annotation.Slf4s
import dev.coilix.context.ApplicationContext
import dev.coilix.context.ClosingPhase
import dev.coilix.context.ContextLifecycleListener

/**
 * Keeps the fibers of the [[dev.coilix.annotation.Async]] methods that return `IO[Unit]`, which run without the caller
 * waiting for them.
 *
 * A failure of such a run is logged. A fiber leaves the registry when its run ends. The fibers that still run are cancelled in
 * the [[dev.coilix.context.ClosingPhase.Producers]] phase of closing, before any bean is destroyed, and a run started after
 * that is skipped with a warning.
 *
 * @param fibers
 *   the running fibers, or `None` once the context is closing
 */
@Slf4s
case class AsyncTasks(fibers: Ref[IO, Option[Map[Unique.Token, FiberIO[Unit]]]]) extends ContextLifecycleListener {

  /** Starts `task` on a fiber of its own and returns without waiting for it. */
  def start(label: String, task: IO[Unit]): IO[Unit] =
    (IO.unique, Deferred[IO, Boolean]).flatMapN { (token, admitted) =>
      IO.uncancelable { _ =>
        guarded(label, task, token, admitted).start.flatMap(fiber => register(label, token, fiber, admitted))
      }
    }

  /** The number of runs that have not ended yet. */
  def running: IO[Int] = fibers.get.map(_.fold(0)(_.size))

  override def onContextStarted(context: ApplicationContext): IO[Unit] = IO.unit

  override def closingPhase: ClosingPhase = ClosingPhase.Producers

  override def onContextClosing: IO[Unit] =
    fibers.getAndSet(None).flatMap(_.fold(IO.unit)(_.values.toList.traverse_(_.cancel)))

  private def guarded(label: String, task: IO[Unit], token: Unique.Token, admitted: Deferred[IO, Boolean]): IO[Unit] =
    admitted.get.ifM(
      task
        .handleErrorWith(failure => logF.error(s"$label failed", failure))
        .guarantee(fibers.update(_.map(_ - token))),
      IO.unit
    )

  private def register(label: String, token: Unique.Token, fiber: FiberIO[Unit], admitted: Deferred[IO, Boolean]): IO[Unit] =
    fibers
      .modify {
        case Some(running) => (Some(running + (token -> fiber)), true)
        case None          => (None, false)
      }
      .flatTap(admitted.complete)
      .flatMap(accepted => if (accepted) IO.unit else logF.warn(s"$label was not started, because the context is closing"))
}

/** Creation of the [[dev.coilix.concurrency.AsyncTasks]]. */
object AsyncTasks {

  /** A registry without running fibers. Creating the `Ref` runs no effect, so the registry is a plain bean. */
  def apply(): AsyncTasks = AsyncTasks(Ref.unsafe[IO, Option[Map[Unique.Token, FiberIO[Unit]]]](Some(Map.empty)))
}
