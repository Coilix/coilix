package dev.coilix.catseffect

import cats.effect.unsafe.IORuntime
import dev.coilix.annotation.Bean
import dev.coilix.annotation.Configuration
import dev.coilix.concurrency.ConcurrencySettings
import dev.coilix.concurrency.ContextPropagator
import dev.coilix.concurrency.FatalErrorHandler
import dev.coilix.config.Environment

/**
 * The beans of the cats-effect module: `coilixIORuntime`, which creates the runtime and closes it with the context, `ioRuntime`
 * for code that needs a `given IORuntime`, and `ioTaskExecutor`, the [[dev.coilix.concurrency.TaskExecutor]] on the compute pool
 * of the runtime, which takes the place of the default executor of the concurrency module, and `ioRunnerAdapter`, which runs the
 * [[dev.coilix.catseffect.IOApplicationRunner]] beans.
 */
@Configuration
final class CatsEffectConfiguration {

  /** The settings under `coilix.cats-effect`, or the defaults in a context without an environment. */
  @Bean
  def catsEffectSettings(environment: Option[Environment]): CatsEffectSettings =
    environment.fold(CatsEffectSettings())(CatsEffectSettings.from)

  /** The runtime with `coilix.concurrency.compute.threads` compute threads. */
  @Bean
  def coilixIORuntime(settings: ConcurrencySettings, fatalErrors: FatalErrorHandler): CoilixIORuntime =
    CoilixIORuntime(settings.compute.threads, fatalErrors)

  /** The cats-effect runtime of the application. */
  @Bean
  def ioRuntime(owner: CoilixIORuntime): IORuntime =
    owner.runtime

  /** Runs the [[dev.coilix.catseffect.IOApplicationRunner]] beans in one list with the other runners. */
  @Bean
  def ioRunnerAdapter(runtime: IORuntime): IORunnerAdapter =
    IORunnerAdapter(runtime)

  /** Runs the tasks of Coilix on the compute pool of the runtime. */
  @Bean
  def ioTaskExecutor(
    owner:       CoilixIORuntime,
    fatalErrors: FatalErrorHandler,
    propagators: List[ContextPropagator]
  ): IORuntimeTaskExecutor =
    IORuntimeTaskExecutor(owner.runtime, fatalErrors, propagators)
}
