package dev.coilix.concurrency

import dev.coilix.annotation.Bean
import dev.coilix.annotation.ConditionalOnMissingBean
import dev.coilix.annotation.ConditionalOnProperty
import dev.coilix.annotation.Configuration
import dev.coilix.annotation.Slf4s
import dev.coilix.config.Environment
import dev.coilix.context.ConfigurableApplicationContext

/**
 * The beans of the concurrency module: `concurrencySettings`, `taskExecutor` for computation, `blockingExecutor` for calls that
 * wait, `taskScheduler`, `scheduledMethodRegistrar`, which runs the [[dev.coilix.annotation.Scheduled]] methods,
 * `mdcContextPropagator`, which carries the MDC to tasks, and `fatalErrorSettings`, `processTerminator` and `fatalErrorHandler`, which react to fatal errors in the threads of the
 * executors and the scheduler.
 *
 * The executors and the scheduler are closed with the context, after the beans that depend on them. `taskExecutor` and
 * `blockingExecutor` are left out when another bean provides a [[dev.coilix.concurrency.TaskExecutor]] or a
 * [[dev.coilix.concurrency.BlockingExecutor]], so their pools are not even created.
 */
@Slf4s
@Configuration
final class ConcurrencyConfiguration {

  /** The settings under `coilix.concurrency`, or the defaults in a context without an environment. */
  @Bean
  def concurrencySettings(environment: Option[Environment]): ConcurrencySettings =
    environment.fold(ConcurrencySettings())(ConcurrencySettings.from)

  /** The compute pool configured under `coilix.concurrency.compute`, one platform thread per processor by default. */
  @Bean
  @ConditionalOnMissingBean(Array(classOf[TaskExecutor]))
  def taskExecutor(
    settings:    ConcurrencySettings,
    fatalErrors: FatalErrorHandler,
    propagators: List[ContextPropagator]
  ): ExecutorServiceTaskExecutor = {
    log.info(s"Task executor ${settings.compute.threadNamePrefix}* computes on ${settings.compute.threads} platform threads")
    ExecutorServiceTaskExecutor(settings.compute, fatalErrors, propagators)
  }

  /** The executor for blocking calls configured under `coilix.concurrency.blocking`, on virtual threads by default. */
  @Bean
  @ConditionalOnMissingBean(Array(classOf[BlockingExecutor]))
  def blockingExecutor(
    settings:    ConcurrencySettings,
    fatalErrors: FatalErrorHandler,
    propagators: List[ContextPropagator]
  ): ExecutorServiceBlockingExecutor = {
    val blocking = settings.blocking
    val threads  =
      if (blocking.virtualThreads && ThreadFactories.virtualThreadsAvailable) "virtual threads"
      else s"up to ${blocking.platformThreads} platform threads"
    val limit    = blocking.maxConcurrency.fold("")(max => s", at most $max at once")
    log.info(s"Blocking executor ${blocking.threadNamePrefix}* waits on $threads$limit")
    ExecutorServiceBlockingExecutor(blocking, fatalErrors, propagators)
  }

  /** The scheduler configured under `coilix.concurrency.scheduler`. */
  @Bean
  def taskScheduler(
    settings:    ConcurrencySettings,
    fatalErrors: FatalErrorHandler,
    propagators: List[ContextPropagator]
  ): ScheduledExecutorTaskScheduler =
    ScheduledExecutorTaskScheduler(settings.scheduler, fatalErrors, propagators)

  /** Carries the MDC of the submitting thread to tasks, unless `coilix.concurrency.propagate-mdc` is `false`. */
  @Bean
  @ConditionalOnProperty(name = "coilix.concurrency.propagate-mdc", havingValue = "true", matchIfMissing = true)
  def mdcContextPropagator(): MdcContextPropagator =
    MdcContextPropagator()

  /** The settings under `coilix.fatal-errors`, or the defaults in a context without an environment. */
  @Bean
  def fatalErrorSettings(environment: Option[Environment]): FatalErrorSettings =
    environment.fold(FatalErrorSettings())(FatalErrorSettings.from)

  /** Ends the JVM process after a fatal error. */
  @Bean
  def processTerminator(): RuntimeProcessTerminator =
    RuntimeProcessTerminator(Runtime.getRuntime)

  /**
   * The reaction to fatal errors chosen by `coilix.fatal-errors.action`: closing the context and ending the process, or only
   * logging.
   */
  @Bean
  def fatalErrorHandler(
    settings:   FatalErrorSettings,
    context:    ConfigurableApplicationContext,
    terminator: ProcessTerminator
  ): FatalErrorHandler =
    settings.action match {
      case FatalErrorAction.Shutdown => ShutdownFatalErrorHandler(settings, context, terminator)
      case FatalErrorAction.Log      => LoggingFatalErrorHandler()
    }

  /** Runs the methods annotated with [[dev.coilix.annotation.Scheduled]] on the task scheduler. */
  @Bean
  def scheduledMethodRegistrar(scheduler: TaskScheduler, environment: Option[Environment]): ScheduledMethodRegistrar =
    ScheduledMethodRegistrar(scheduler, environment)
}
