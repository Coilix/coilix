package dev.coilix.processing

/**
 * A processor that starts its work on a shard by itself and reports it as an [[dev.coilix.processing.Execution]], for
 * work that already runs on its own threads, such as a stream or a client with a callback.
 *
 * The manager stops the work with [[dev.coilix.processing.Execution.stop]] and waits for its completion. A completion
 * with a failure goes to the restart policy.
 */
trait CustomProcessor extends ProcessorDefinition {

  /** Starts the work on the shard and returns without waiting for it. */
  def run(shard: Shard): Execution
}
