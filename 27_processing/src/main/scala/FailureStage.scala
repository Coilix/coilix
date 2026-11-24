package dev.coilix.processing

/** Where the work on a shard failed. */
enum FailureStage {

  /** In [[dev.coilix.processing.ProcessorDefinition.prepare]]. */
  case Prepare

  /** In the work of the processor itself. */
  case Process
}
