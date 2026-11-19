package dev.coilix.concurrency

import scala.concurrent.duration.DurationInt
import scala.concurrent.duration.FiniteDuration

/**
 * Settings of the `taskExecutor` bean, read from `coilix.concurrency.executor`.
 *
 * ```yaml
 * coilix.concurrency.executor:
 *   virtual-threads: false
 *   pool-size: 8
 * ```
 *
 * @param threadNamePrefix
 *   the beginning of each thread name, followed by a number
 * @param virtualThreads
 *   whether each task runs on a new virtual thread, or on a fixed pool of platform threads
 * @param poolSize
 *   the number of platform threads when `virtualThreads` is `false`, by default the number of processors
 * @param shutdownTimeout
 *   how long closing the context waits for started tasks before interrupting them
 */
final case class ExecutorSettings(
  threadNamePrefix: String         = "coilix-task-",
  virtualThreads:   Boolean        = true,
  poolSize:         Int            = Runtime.getRuntime.availableProcessors,
  shutdownTimeout:  FiniteDuration = 30.seconds
)
