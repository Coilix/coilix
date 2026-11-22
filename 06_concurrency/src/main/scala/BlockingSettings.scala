package dev.coilix.concurrency

import scala.concurrent.duration.DurationInt
import scala.concurrent.duration.FiniteDuration

/**
 * Settings of the default `blockingExecutor` bean, read from `coilix.concurrency.blocking`.
 *
 * ```yaml
 * coilix.concurrency.blocking:
 *   max-concurrency: 20
 * ```
 *
 * @param virtualThreads
 *   whether each task runs on a new virtual thread when the JDK has them, or on a pool of platform threads
 * @param maxConcurrency
 *   how many tasks may run at once, for example the size of the connection pool they use, or `None` for no limit
 * @param platformThreads
 *   the largest number of platform threads when virtual threads are not used
 * @param threadNamePrefix
 *   the beginning of each thread name, followed by a number
 * @param shutdownTimeout
 *   how long closing the context waits for started tasks before interrupting them
 */
final case class BlockingSettings(
  virtualThreads:   Boolean        = true,
  maxConcurrency:   Option[Int]    = None,
  platformThreads:  Int            = 64,
  threadNamePrefix: String         = "coilix-blocking-",
  shutdownTimeout:  FiniteDuration = 30.seconds
)
