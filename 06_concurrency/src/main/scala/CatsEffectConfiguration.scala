package dev.coilix.concurrency

import cats.effect.unsafe.IORuntime
import dev.coilix.annotation.Bean
import dev.coilix.annotation.ConditionalOnMissingBean
import dev.coilix.annotation.Configuration
import dev.coilix.config.Environment

/**
 * The cats-effect beans of the concurrency module: `coilixIORuntime`, which creates the runtime and closes it with the context,
 * `ioRuntime` for code that needs a `given IORuntime`, and `ioTaskExecutor`, the [[dev.coilix.concurrency.TaskExecutor]] on
 * the compute pool of the runtime, left out when the application provides its own executor.
 *
 * When the context already has an `IORuntime`, such as the runtime of a `CoilixIOApp` of the boot module, neither
 * `coilixIORuntime` nor `ioRuntime` is created and the other beans use that runtime.
 */
@Configuration
final class CatsEffectConfiguration {

  /** The settings under `coilix.cats-effect`, or the defaults in a context without an environment. */
  @Bean
  def catsEffectSettings(environment: Option[Environment]): CatsEffectSettings =
    environment.fold(CatsEffectSettings())(CatsEffectSettings.from)

  /** The runtime with `coilix.concurrency.compute.threads` compute threads. */
  @Bean
  @ConditionalOnMissingBean(Array(classOf[IORuntime]))
  def coilixIORuntime(settings: ConcurrencySettings, fatalErrors: FatalErrorHandler): CoilixIORuntime =
    CoilixIORuntime(settings.compute.threads, fatalErrors)

  /** The cats-effect runtime of the application. */
  @Bean
  @ConditionalOnMissingBean(Array(classOf[IORuntime]))
  def ioRuntime(owner: CoilixIORuntime): IORuntime =
    owner.runtime

  /** Runs the tasks of Coilix on the compute pool of the runtime, unless another bean provides a `TaskExecutor`. */
  @Bean
  @ConditionalOnMissingBean(Array(classOf[TaskExecutor]))
  def ioTaskExecutor(
    runtime:     IORuntime,
    fatalErrors: FatalErrorHandler,
    propagators: List[ContextPropagator]
  ): IORuntimeTaskExecutor =
    IORuntimeTaskExecutor(runtime, fatalErrors, propagators)
}
