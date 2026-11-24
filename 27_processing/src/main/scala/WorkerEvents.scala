package dev.coilix.processing

/**
 * Where a [[dev.coilix.processing.ProcessorEngine]] reports what happens to one attempt of the work.
 *
 * Each method returns at once. An engine reports `prepared` once, then exactly one of `finished` and `crashed`, also when
 * the work ends because it was stopped.
 */
trait WorkerEvents {

  /** [[dev.coilix.processing.ProcessorDefinition.prepare]] succeeded and the work started. */
  def prepared(): Unit

  /** The work ended without a failure. */
  def finished(): Unit

  /** The work failed at the given stage. */
  def crashed(stage: FailureStage, error: Throwable): Unit
}
