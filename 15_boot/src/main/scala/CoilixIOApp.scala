package dev.coilix.boot

import cats.effect.ExitCode
import cats.effect.IO
import cats.effect.IOApp
import cats.effect.metrics.CpuStarvationWarningMetrics
import dev.coilix.annotation.Slf4s
import dev.coilix.concurrency.CatsEffectSettings
import dev.coilix.concurrency.ConcurrencySettings
import dev.coilix.config.Environment
import dev.coilix.config.EnvironmentLoader
import scala.reflect.ClassTag

/**
 * Entry point of a Coilix application: an `IOApp` of cats-effect whose context lives as long as its main fiber.
 *
 * ```scala
 * @CoilixApplication
 * object ShopApplication extends CoilixIOApp
 * ```
 *
 * The runtime of the `IOApp`, with `coilix.concurrency.compute.threads` compute threads, becomes the `ioRuntime` bean, so it is the
 * only compute pool of the application. `run` logs the process identifier and starts the application as described in
 * [[dev.coilix.boot.CoilixIOStartup]]: it builds the context, runs the [[dev.coilix.boot.IOApplicationRunner]] and
 * [[dev.coilix.boot.ApplicationRunner]] beans and waits until the application is cancelled, for example by `SIGTERM`, which
 * closes the context. With `coilix.cats-effect.exit-after-runners = true` the application closes and ends after its runners.
 * A failure of the start or of a runner is logged once and ends the application with exit code 1.
 *
 * Warnings of the runtime about a starved compute pool and failures it reports go to the log instead of the standard error.
 * `coilix.cats-effect.blocked-thread-detection = true` makes the runtime warn when a compute thread blocks, at a small cost.
 *
 * The number of compute threads and the detection of blocked threads are read before the command-line arguments are known, so
 * they come from the configuration files, system properties and environment variables, not from command-line options.
 */
trait CoilixIOApp extends IOApp {

  private lazy val environment: Environment = EnvironmentLoader.default.load(Map.empty)

  /** The number of compute threads from `coilix.concurrency.compute.threads`, by default one per processor. */
  override protected def computeWorkerThreadCount: Int =
    ConcurrencySettings.from(environment).compute.threads

  /** Whether the runtime warns about blocked compute threads, from `coilix.cats-effect.blocked-thread-detection`. */
  override protected def blockedThreadDetectionEnabled: Boolean =
    CatsEffectSettings.from(environment).blockedThreadDetection

  /** Logs a warning when the compute pool is starved, for example by blocking calls outside `IO.blocking`. */
  override protected def onCpuStarvationWarn(metrics: CpuStarvationWarningMetrics): IO[Unit] =
    IO(
      CoilixIOApp.log.warn(
        s"The compute pool is starved: a fiber waited ${metrics.clockDrift.toMillis} ms longer than expected " +
          s"(threshold ${metrics.starvationThreshold}); look for blocking calls outside IO.blocking"
      )
    )

  /** Logs a failure that the runtime cannot hand to any fiber. */
  override protected def reportFailure(error: Throwable): IO[Unit] =
    IO(CoilixIOApp.log.error("A failure escaped the error handling of a fiber", error))

  /** Starts the application annotated with `@CoilixApplication` that extends this trait. */
  final override def run(args: List[String]): IO[ExitCode] =
    IO(CoilixIOApp.log.info(s"$applicationName runs as process ${ProcessHandle.current.pid} on Java ${Runtime.version}")) *>
      CoilixIOStartup
        .run(args, runtime)(using ClassTag(getClass))
        .onCancel(IO(CoilixIOApp.log.info(s"$applicationName was asked to stop and closes")))
        .handleErrorWith(failed)

  private def failed(error: Throwable): IO[ExitCode] =
    IO(CoilixIOApp.log.error(s"$applicationName ended with an error: ${error.getMessage}", error)).as(ExitCode.Error)

  private def applicationName: String = getClass.getSimpleName.stripSuffix("$")
}

/** The logger of [[dev.coilix.boot.CoilixIOApp]]. */
@Slf4s
object CoilixIOApp
