package dev.coilix.processing

import cats.data.EitherT
import cats.effect.IO
import scala.concurrent.duration.FiniteDuration

/**
 * The only bean of the `processing` module and the only way to control background processors.
 *
 * The manager creates every class with the `@Processor` annotation once, assigns its shards to the replicas of the
 * application and supervises a worker for each shard of this replica. Operations take effect on this replica, one at a
 * time on the control fiber of the manager, so they are safe to run from many fibers. They are idempotent: starting a
 * running processor or stopping a stopped one changes nothing.
 *
 * ```scala
 * @Component
 * final class Maintenance(processors: ProcessorManager) {
 *   def pauseExports: IO[StopResult] = ProcessorError.raised(processors.stop(ShopProcessor.InvoiceExport))
 *
 *   def resumeExports: EitherT[IO, ProcessorError, Unit] = processors.start(ShopProcessor.InvoiceExport)
 * }
 * ```
 *
 * Every operation that takes an identifier returns [[dev.coilix.processing.ProcessorError.NoSuchProcessor]] when no
 * processor has it. The caller decides what to do with the error: handle it, or fail its effect with
 * [[dev.coilix.processing.ProcessorError.raised]].
 */
trait ProcessorManager {

  /** Every processor of the application, ordered by name. */
  def processors: List[ProcessorDescriptor]

  /** The processors with the shards of this replica. */
  def topology: ProcessingTopology

  /** The state of the shards of the processor on this replica. Reading it does not wait for running operations. */
  def status(id: ProcessorId): EitherT[IO, ProcessorError, ProcessorStatus]

  /** Starts the shards of the processor that are not running. A `Failed` shard starts with a fresh restart policy. */
  def start(id: ProcessorId): EitherT[IO, ProcessorError, Unit]

  /** Stops the shards of the processor and waits for them at most `coilix.processors.shutdown-timeout`. */
  def stop(id: ProcessorId): EitherT[IO, ProcessorError, StopResult]

  /** Stops the shards of the processor and waits for them at most `timeout`. */
  def stop(id: ProcessorId, timeout: FiniteDuration): EitherT[IO, ProcessorError, StopResult]

  /** Stops the running shards of the processor and starts every shard again. */
  def restart(id: ProcessorId): EitherT[IO, ProcessorError, Unit]

  /**
   * Stops the shard if it runs and starts it again. Returns [[dev.coilix.processing.ProcessorError.ShardNotOwned]] when
   * this replica does not run the shard.
   */
  def restart(id: ProcessorId, shard: Int): EitherT[IO, ProcessorError, Unit]

  /** Stops every processor and waits for the shards at most `timeout`. */
  def stopAll(timeout: FiniteDuration): IO[List[StopResult]]
}
