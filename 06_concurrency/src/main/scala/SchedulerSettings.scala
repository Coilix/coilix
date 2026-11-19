package dev.coilix.concurrency

import scala.concurrent.duration.DurationInt
import scala.concurrent.duration.FiniteDuration

/**
 * Settings of the `taskScheduler` bean, read from `coilix.concurrency.scheduler`.
 *
 * @param threadNamePrefix
 *   the beginning of each thread name, followed by a number
 * @param poolSize
 *   the number of platform threads that run scheduled tasks
 * @param shutdownTimeout
 *   how long closing the context waits for running tasks before interrupting them
 */
final case class SchedulerSettings(
  threadNamePrefix: String         = "coilix-scheduler-",
  poolSize:         Int            = 1,
  shutdownTimeout:  FiniteDuration = 10.seconds
)
