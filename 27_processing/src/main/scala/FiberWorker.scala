package dev.coilix.processing

import cats.effect.Fiber
import cats.effect.IO
import cats.effect.std.Supervisor
import java.util.concurrent.TimeoutException
import scala.concurrent.duration.FiniteDuration

/**
 * One attempt of the work of an [[dev.coilix.processing.IOProcessor]] on its own fiber.
 *
 * The fiber runs [[dev.coilix.processing.ProcessorDefinition.prepare]] and then the work. Stopping completes
 * [[dev.coilix.processing.Shard.stopped]], waits half of the timeout for the work to end, cancels it and waits the other
 * half. Work that is still not cancelled then, for example because it runs in an uncancelable region, is abandoned.
 */
case class FiberWorker(shard: Shard, fiber: Fiber[IO, Throwable, Unit]) extends RunningWorker {

  override def stop(timeout: FiniteDuration): IO[StopOutcome] = {
    val half = timeout / 2
    shard.requestStop *> fiber.join.timeout(half).as(StopOutcome.Stopped).recoverWith {
      case _: TimeoutException => cancelled(timeout - half)
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

  /** Starts the attempt on a fiber of `supervisor` and completes without waiting for it. */
  def start(supervisor: Supervisor[IO], processor: IOProcessor, shard: Shard, events: WorkerEvents): IO[FiberWorker] =
    supervisor.supervise(attempt(processor, shard, events)).map(FiberWorker(shard, _))

  private def attempt(processor: IOProcessor, shard: Shard, events: WorkerEvents): IO[Unit] =
    processor.prepare(shard).attempt.flatMap {
      case Left(error) => events.crashed(FailureStage.Prepare, error)
      case Right(_)    => events.prepared *> work(processor, shard, events)
    }

  private def work(processor: IOProcessor, shard: Shard, events: WorkerEvents): IO[Unit] =
    processor.run(shard).attempt.flatMap {
      case Left(error) => events.crashed(FailureStage.Process, error)
      case Right(_)    => events.finished
    }
}
