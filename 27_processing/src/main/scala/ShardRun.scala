package dev.coilix.processing

import dev.coilix.concurrency.ScheduledTask

/**
 * The state of one shard in the manager.
 *
 * @param supervision
 *   the supervision of the shard
 * @param shard
 *   the shard given to the latest worker
 * @param generation
 *   the number of the latest worker, so reports of older workers are ignored
 * @param worker
 *   the running worker
 * @param restart
 *   the scheduled restart
 */
private[processing] final case class ShardRun(
  supervision: ShardSupervision,
  shard:       Shard,
  generation:  Long                  = 0L,
  worker:      Option[RunningWorker] = None,
  restart:     Option[ScheduledTask] = None
)
