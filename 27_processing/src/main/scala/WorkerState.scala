package dev.coilix.processing

/** The state of the worker of one shard on this replica. */
enum WorkerState {

  /** Assigned to this replica, never started. */
  case Idle

  /** Running [[dev.coilix.processing.ProcessorDefinition.prepare]]. */
  case Preparing

  /** Doing the work of the processor. */
  case Running

  /** Waiting for the delay of the restart policy before it starts again. */
  case Backoff

  /** Waiting for the work to stop. */
  case Stopping

  /** Stopped through the [[dev.coilix.processing.ProcessorManager]] or by the closing of the application. */
  case Stopped

  /** The work ended and is not started again. */
  case Completed

  /** The work failed more often than the restart policy allows. */
  case Failed

  /** Whether the shard has a worker or waits to start one again. */
  def isActive: Boolean = this == Preparing || this == Running || this == Backoff || this == Stopping
}
