package dev.coilix.processing

/** What happened to the worker of a shard, the input of [[dev.coilix.processing.ShardSupervision]]. */
enum WorkerEvent {

  /** The shard was asked to start. */
  case StartRequested

  /** The shard was asked to start again, whatever its state. */
  case RestartRequested

  /** The worker finished [[dev.coilix.processing.ProcessorDefinition.prepare]] and started the work. */
  case Prepared

  /** The work ended by itself. */
  case Finished

  /** The work failed. */
  case Crashed(failure: FailureInfo)

  /** The delay before a restart has passed. */
  case RestartDue

  /** The shard was asked to stop for the given reason. */
  case StopRequested(reason: ReleaseReason)

  /** The worker that was asked to stop has stopped or was abandoned. */
  case WorkerStopped
}
