package dev.coilix.processing

import cats.effect.Deferred
import cats.effect.IO
import cats.effect.IOLocal
import cats.effect.Resource
import cats.effect.std.Queue
import dev.coilix.annotation.Slf4s

/**
 * One fiber that runs the operations of the processor manager in order, so the state of the manager changes one operation
 * at a time.
 *
 * An operation called from the loop itself, such as an operation of the manager in a callback of a processor, runs at once
 * instead of waiting behind itself. Fibers started by an operation must be started through
 * [[dev.coilix.processing.ControlLoop.outside]], so their own calls wait in the queue like any other.
 */
@Slf4s
private[processing] case class ControlLoop(queue: Queue[IO, IO[Unit]], inside: IOLocal[Boolean]) {

  /** Runs the task on the loop and waits for its result, failing with its error. */
  def call[A](task: IO[A]): IO[A] =
    inside.get.ifM(task, queued(task))

  /** Runs the task on the loop later, logging its failure. */
  def post(task: IO[Unit]): IO[Unit] =
    queue.offer(task.handleErrorWith(error => IO(log.error("An operation of the processor manager failed", error))))

  /** Runs the task so that fibers it starts are not taken for the loop. */
  def outside[A](task: IO[A]): IO[A] =
    inside.get.flatMap(current => (inside.set(false) *> task).guarantee(inside.set(current)))

  private def queued[A](task: IO[A]): IO[A] =
    Deferred[IO, Either[Throwable, A]].flatMap { result =>
      queue.offer(task.attempt.flatMap(result.complete).void) *> result.get.rethrow
    }

  private def run: IO[Nothing] =
    inside.set(true) *> queue.take.flatMap(identity).foreverM
}

/** Creation of the [[dev.coilix.processing.ControlLoop]]. */
private[processing] object ControlLoop {

  /** A loop whose fiber runs while the resource is in use. */
  def resource: Resource[IO, ControlLoop] =
    for {
      queue  <- Resource.eval(Queue.unbounded[IO, IO[Unit]])
      inside <- Resource.eval(IOLocal(false))
      loop    = ControlLoop(queue, inside)
      _      <- loop.run.background
    } yield loop
}
