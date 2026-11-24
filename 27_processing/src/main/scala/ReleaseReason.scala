package dev.coilix.processing

/** Why this replica gave up a shard, passed to [[dev.coilix.processing.ProcessorDefinition.onReleased]]. */
enum ReleaseReason {

  /** The application is closing. */
  case Shutdown

  /** The processor or the shard was stopped through the [[dev.coilix.processing.ProcessorManager]]. */
  case Stopped

  /** The work ended and `on-completion = stop` does not start it again. */
  case Completed

  /** The work failed more often than the restart policy allows. */
  case Failed
}
