package dev.coilix.processing

import scala.concurrent.Future

/** The running work of a [[dev.coilix.processing.CustomProcessor]] on one shard. */
trait Execution {

  /** Asks the work to stop. It returns at once, and the work completes [[dev.coilix.processing.Execution.completion]]. */
  def stop(): Unit

  /** Completes when the work ends, with a failure when it failed. */
  def completion: Future[Unit]
}
