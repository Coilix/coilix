package dev.coilix.concurrency

import dev.coilix.annotation.Bean
import dev.coilix.annotation.Configuration
import dev.coilix.annotation.Slf4s
import dev.coilix.config.Environment
import dev.coilix.context.ConfigurableApplicationContext

/**
 * The beans of the concurrency module: `concurrencySettings`, `taskExecutor`, `taskScheduler`, `scheduledMethodRegistrar`,
 * which runs the [[dev.coilix.annotation.Scheduled]] methods, and `fatalErrorSettings`, `processTerminator` and
 * `fatalErrorHandler`, which react to fatal errors in the threads of the executor and the scheduler.
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
  def taskExecutor(settings: ConcurrencySettings, fatalErrors: FatalErrorHandler): ExecutorServiceTaskExecutor = {
    val virtual = settings.executor.virtualThreads && ThreadFactories.virtualThreadsAvailable
    log.info(s"Task executor ${settings.executor.threadNamePrefix}* runs on ${if (virtual) "virtual" else "platform"} threads")
    ExecutorServiceTaskExecutor(settings.executor, fatalErrors)
  }

  /** The scheduler configured under `coilix.concurrency.scheduler`. */
  @Bean
  def taskScheduler(settings: ConcurrencySettings, fatalErrors: FatalErrorHandler): ScheduledExecutorTaskScheduler =
    ScheduledExecutorTaskScheduler(settings.scheduler, fatalErrors)

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
