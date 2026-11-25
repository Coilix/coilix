package dev.coilix.processing

import cats.effect.IO
import cats.effect.Resource
import cats.effect.std.Supervisor

/**
 * The engine of the `processing` module: each shard works on its own fiber of the cats-effect runtime of the application.
 *
 * It runs [[dev.coilix.processing.IOProcessor]]. The fibers belong to a `Supervisor`, so the ones still running are
 * cancelled when the engine closes with the context.
 */
case class FiberEngine(supervisor: Supervisor[IO]) extends ProcessorEngine {

  override def name: String = "fibers"

  override def supports(definition: ProcessorDefinition): Boolean =
    definition match {
      case _: IOProcessor => true
      case _ => false
    }

  override def start(definition: ProcessorDefinition, shard: Shard, events: WorkerEvents): IO[RunningWorker] =
    definition match {
      case processor: IOProcessor => FiberWorker.start(supervisor, processor, shard, events)
      case _ => IO.raiseError(IllegalArgumentException(s"$name does not run ${definition.getClass.getName}"))
    }
}

/** Creation of the [[dev.coilix.processing.FiberEngine]]. */
object FiberEngine {

  /** An engine whose fibers are cancelled when the resource is released. */
  def resource: Resource[IO, FiberEngine] =
    Supervisor[IO](await = false).map(FiberEngine(_))
}
