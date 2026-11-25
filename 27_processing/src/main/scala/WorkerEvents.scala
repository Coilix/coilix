package dev.coilix.processing

import cats.effect.IO

/**
 * Where a [[dev.coilix.processing.ProcessorEngine]] reports what happens to one attempt of the work.
 *
 * Each report completes at once. An engine reports `prepared` once, then exactly one of `finished` and `crashed`, unless
 * the work is cancelled while it stops.
 */
trait WorkerEvents {

  /** [[dev.coilix.processing.ProcessorDefinition.prepare]] succeeded and the work started. */
  def prepared: IO[Unit]

  /** The work ended without a failure. */
  def finished: IO[Unit]

  /** The work failed at the given stage. */
  def crashed(stage: FailureStage, error: Throwable): IO[Unit]
}
