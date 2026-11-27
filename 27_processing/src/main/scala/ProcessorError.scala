package dev.coilix.processing

import cats.data.EitherT
import cats.effect.IO
import dev.coilix.common.CoilixException

/**
 * A predictable reason why an operation of the [[dev.coilix.processing.ProcessorManager]] cannot be done, returned as the left
 * side of its operations.
 *
 * The caller decides what to do with it, for example answer an HTTP request with `404`, or fail its effect with the
 * [[dev.coilix.processing.ProcessorError.exception]] through [[dev.coilix.processing.ProcessorError.raised]].
 */
enum ProcessorError {

  /**
   * No class with `@Processor` has the identifier.
   *
   * @param id
   *   the requested identifier
   */
  case NoSuchProcessor(id: ProcessorId)

  /**
   * This replica does not run the shard.
   *
   * @param id
   *   the processor
   * @param shard
   *   the requested shard
   * @param owners
   *   the replicas that run the shard, empty when the processor has no such shard
   */
  case ShardNotOwned(id: ProcessorId, shard: Int, owners: List[Int])

  /** The exception that reports this error to code that throws. */
  def exception: CoilixException =
    this match {
      case NoSuchProcessor(id)              => NoSuchProcessorException(id)
      case ShardNotOwned(id, shard, owners) => ShardNotOwnedException(id, shard, owners)
    }

  /** The description of this error, the message of its [[dev.coilix.processing.ProcessorError.exception]]. */
  def message: String = exception.getMessage
}

/** Raising a [[dev.coilix.processing.ProcessorError]] for callers that cannot handle it. */
object ProcessorError {

  /** The value of `operation`, failing the effect with the [[dev.coilix.processing.ProcessorError.exception]] of an error. */
  def raised[A](operation: EitherT[IO, ProcessorError, A]): IO[A] =
    operation.foldF(error => IO.raiseError(error.exception), IO.pure)
}
