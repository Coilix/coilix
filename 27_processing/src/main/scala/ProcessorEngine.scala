package dev.coilix.processing

/**
 * Runs the work of one shard in its model of concurrency, such as virtual threads, fibers of `IO` or graphs of Pekko.
 *
 * The [[dev.coilix.processing.ProcessorManager]] picks the only engine that supports a processor; none or several is an
 * error at startup. An engine runs one attempt of the work at a time and reports what happens to the given
 * [[dev.coilix.processing.WorkerEvents]]. Restarts, delays and stopping on closing are decided by the manager, so they
 * are the same for every engine. Modules that add a style of processors add their engine as a bean.
 */
trait ProcessorEngine {

  /** A short name of the engine for logs and errors, such as `virtual-threads`. */
  def name: String

  /** Whether this engine runs the processor. */
  def supports(definition: ProcessorDefinition): Boolean

  /**
   * Starts one attempt of the work on the shard, beginning with [[dev.coilix.processing.ProcessorDefinition.prepare]],
   * and returns without waiting for it.
   */
  def start(definition: ProcessorDefinition, shard: Shard, events: WorkerEvents): RunningWorker
}
