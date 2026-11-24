package dev.coilix.processing

import scala.concurrent.duration.FiniteDuration

/** One attempt of the work on a shard, started by a [[dev.coilix.processing.ProcessorEngine]]. */
trait RunningWorker {

  /** Stops the work and waits for it at most `timeout`. */
  def stop(timeout: FiniteDuration): StopOutcome
}
