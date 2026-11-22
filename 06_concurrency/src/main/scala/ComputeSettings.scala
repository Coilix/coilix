package dev.coilix.concurrency

import scala.concurrent.duration.DurationInt
import scala.concurrent.duration.FiniteDuration

/**
 * Settings of the compute pool behind the default `taskExecutor` bean, read from `coilix.concurrency.compute`.
 *
 * ```yaml
 * coilix.concurrency.compute:
 *   threads: 8
 * ```
 *
 * @param threads
 *   the number of platform threads, by default the number of processors available to the JVM, which in a container is its CPU
 *   limit
 * @param threadNamePrefix
 *   the beginning of each thread name, followed by a number
 * @param shutdownTimeout
 *   how long closing the context waits for started tasks before interrupting them
 */
final case class ComputeSettings(
  threads:          Int            = Runtime.getRuntime.availableProcessors,
  threadNamePrefix: String         = "coilix-compute-",
  shutdownTimeout:  FiniteDuration = 30.seconds
)
