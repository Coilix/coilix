package dev.coilix.processing

import scala.concurrent.duration.FiniteDuration

/** What the manager does after a step of [[dev.coilix.processing.ShardSupervision]], in the given order. */
enum WorkerCommand {

  /** Call [[dev.coilix.processing.ProcessorDefinition.onAcquired]]. */
  case Acquire

  /** Start a new worker through the engine of the processor. */
  case StartWorker

  /** Stop the current worker and wait for it. */
  case StopWorker

  /** Send [[dev.coilix.processing.WorkerEvent.RestartDue]] after the delay. */
  case ScheduleRestart(delay: FiniteDuration)

  /** Cancel the scheduled restart. */
  case CancelRestart

  /** Call [[dev.coilix.processing.ProcessorDefinition.onReleased]] with the reason. */
  case Release(reason: ReleaseReason)
}
