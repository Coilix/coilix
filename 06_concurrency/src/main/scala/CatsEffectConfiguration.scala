package dev.coilix.concurrency

import cats.effect.Async
import cats.effect.IO
import cats.effect.unsafe.IORuntime
import dev.coilix.annotation.Bean
import dev.coilix.annotation.ConditionalOnMissingBean
import dev.coilix.annotation.Configuration
import dev.coilix.config.Environment

/**
 * The cats-effect beans of the concurrency module: `coilixIORuntime`, which creates the runtime and closes it with the context,
 * `ioRuntime` for code that needs a `given IORuntime`, `ioTaskExecutor`, the [[dev.coilix.concurrency.TaskExecutor]] on the
 * compute pool of the runtime, left out when the application provides its own executor, and `ioAsync`, the `Async[IO]` instance that
 * tagless final components receive for their `Sync[F]`, `Temporal[F]`, `Concurrent[F]` or `Async[F]` context bounds.
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

  /**
   * The `Async` instance of `IO`. A dependency on any of its supertypes, such as `Sync[IO]` or `Temporal[IO]`, receives it, so
   * the context bounds of tagless final components resolve like other dependencies.
   */
  @Bean
  def ioAsync(): Async[IO] = IO.asyncForIO

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
