package dev.coilix.concurrency

import dev.coilix.annotation.Bean
import dev.coilix.annotation.Configuration
import dev.coilix.annotation.Slf4s
import dev.coilix.config.Environment

/**
 * The beans of the concurrency module: `concurrencySettings`, `taskExecutor`, `taskScheduler` and
 * `scheduledMethodRegistrar`, which runs the [[dev.coilix.annotation.Scheduled]] methods.
 *
 * The executor and the scheduler are closed with the context, after the beans that depend on them.
 */
@Slf4s
@Configuration
final class ConcurrencyConfiguration {

  /** The settings under `coilix.concurrency`, or the defaults in a context without an environment. */
  @Bean
  def concurrencySettings(environment: Option[Environment]): ConcurrencySettings =
    environment.fold(ConcurrencySettings())(ConcurrencySettings.from)

  /** The executor configured under `coilix.concurrency.executor`, on virtual threads by default. */
  @Bean
  def taskExecutor(settings: ConcurrencySettings): ExecutorServiceTaskExecutor = {
    val virtual = settings.executor.virtualThreads && ThreadFactories.virtualThreadsAvailable
    log.info(s"Task executor ${settings.executor.threadNamePrefix}* runs on ${if (virtual) "virtual" else "platform"} threads")
    ExecutorServiceTaskExecutor(settings.executor)
  }

  /** The scheduler configured under `coilix.concurrency.scheduler`. */
  @Bean
  def taskScheduler(settings: ConcurrencySettings): ScheduledExecutorTaskScheduler =
    ScheduledExecutorTaskScheduler(settings.scheduler)

  /** Runs the methods annotated with [[dev.coilix.annotation.Scheduled]] on the task scheduler. */
  @Bean
  def scheduledMethodRegistrar(scheduler: TaskScheduler, environment: Option[Environment]): ScheduledMethodRegistrar =
    ScheduledMethodRegistrar(scheduler, environment)
}
