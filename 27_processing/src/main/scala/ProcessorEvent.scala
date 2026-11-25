package dev.coilix.processing

import java.time.Instant

/**
 * What happened to a shard of a processor on this replica, published through the [[dev.coilix.events.EventPublisher]] of the
 * context on the thread of the processor manager. A listener that is slow, such as one that sends alerts, should be
 * asynchronous, so that it does not hold up the manager:
 *
 * ```scala
 * @EventListener(async = true)
 * def onFailure(event: ProcessorEvent.ShardFailed): Unit =
 *   alerts.send(s"${event.processor.name}#${event.shard} failed: ${event.failure.message}")
 * ```
 */
enum ProcessorEvent {

  /** The worker of the shard went from `previous` to `current`, for example from `Running` to `Backoff` after a failure. */
  case ShardStateChanged(processor: ProcessorId, shard: Int, previous: WorkerState, current: WorkerState, at: Instant)

  /** The worker of the shard failed; the restart policy decides what comes next. */
  case ShardFailed(processor: ProcessorId, shard: Int, failure: FailureInfo)
}
