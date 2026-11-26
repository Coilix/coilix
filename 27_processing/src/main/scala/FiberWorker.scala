package dev.coilix.processing

import cats.effect.Fiber
import cats.effect.IO
import cats.effect.std.Supervisor
import java.util.concurrent.TimeoutException
import scala.concurrent.duration.DurationInt
import scala.concurrent.duration.FiniteDuration

/**
 * One attempt of the work of an [[dev.coilix.processing.IOProcessor]] or a [[dev.coilix.processing.StreamProcessor]] on its
 * own fiber.
 *
 * The fiber runs [[dev.coilix.processing.ProcessorDefinition.prepare]] and then the work. Stopping completes
 * [[dev.coilix.processing.Shard.stopped]] and waits for the work to end for [[dev.coilix.processing.Shard.drainTimeout]], by
 * default half of the stop timeout and at most all of it. Then it cancels the work and waits the rest of the timeout, at least
 * one second, for its finalizers. Work that is still not cancelled then, for example because it runs in an uncancelable region,
 * is abandoned.
 */
case class FiberWorker(shard: Shard, fiber: Fiber[IO, Throwable, Unit]) extends RunningWorker {

  private val cancelGrace = 1.second

  override def stop(timeout: FiniteDuration): IO[StopOutcome] = {
    val drain = shard.drainTimeout.getOrElse(timeout / 2).min(timeout)
    shard.requestStop *> fiber.join.timeout(drain).as(StopOutcome.Stopped).recoverWith {
      case _: TimeoutException => cancelled((timeout - drain).max(cancelGrace))
    }
  }

  private def cancelled(timeout: FiniteDuration): IO[StopOutcome] =
    fiber.cancel.start.flatMap(_.join.timeout(timeout)).as(StopOutcome.Stopped).recover {
      case _: TimeoutException =>
        StopOutcome.Abandoned
    }
}

/** Starting a [[dev.coilix.processing.FiberWorker]]. */
object FiberWorker {

  /** Starts `work`, the work of `definition` on `shard`, on a fiber of `supervisor` and completes without waiting for it. */
  def start(
    supervisor: Supervisor[IO],
    definition: ProcessorDefinition,
    work:       IO[Unit],
    shard:      Shard,
    events:     WorkerEvents
  ): IO[FiberWorker] =
    supervisor.supervise(attempt(definition, work, shard, events)).map(FiberWorker(shard, _))

  private def attempt(definition: ProcessorDefinition, work: IO[Unit], shard: Shard, events: WorkerEvents): IO[Unit] =
    definition.prepare(shard).attempt.flatMap {
      case Left(error) => events.crashed(FailureStage.Prepare, error)
      case Right(_)    => events.prepared *> worked(work, events)
    }

  private def worked(work: IO[Unit], events: WorkerEvents): IO[Unit] =
    work.attempt.flatMap {
      case Left(error) => events.crashed(FailureStage.Process, error)
      case Right(_)    => events.finished
    }
}
