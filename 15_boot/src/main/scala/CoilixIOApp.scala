package dev.coilix.boot

import cats.effect.ExitCode
import cats.effect.IO
import cats.effect.IOApp
import dev.coilix.concurrency.ConcurrencySettings
import dev.coilix.config.EnvironmentLoader
import scala.reflect.ClassTag

/**
 * Entry point of a Coilix application written with cats-effect: an `IOApp` whose context lives as long as its main fiber.
 *
 * ```scala
 * @CoilixApplication
 * object ShopApplication extends CoilixIOApp
 * ```
 *
 * The runtime of the `IOApp`, with `coilix.concurrency.compute.threads` compute threads, becomes the `ioRuntime` bean, so it is the
 * only compute pool of the application. `run` starts the application as described in [[dev.coilix.boot.CoilixIOStartup]]:
 * it builds the context, runs the [[dev.coilix.boot.IOApplicationRunner]] and [[dev.coilix.boot.ApplicationRunner]] beans
 * and waits until the application is cancelled, for example by `SIGTERM`, which closes the context. With
 * `coilix.cats-effect.exit-after-runners = true` the application closes and ends after its runners.
 *
 * The number of compute threads is read before the command-line arguments are known, so it comes from the configuration files,
 * system properties and environment variables, not from a `--coilix.concurrency.compute.threads` option.
 */
trait CoilixIOApp extends IOApp {

  /** The number of compute threads from `coilix.concurrency.compute.threads`, by default one per processor. */
  override protected def computeWorkerThreadCount: Int =
    ConcurrencySettings.from(EnvironmentLoader.default.load(Map.empty)).compute.threads

  /** Starts the application annotated with `@CoilixApplication` that extends this trait. */
  final override def run(args: List[String]): IO[ExitCode] =
    CoilixIOStartup.run(args, runtime)(using ClassTag(getClass))
}
