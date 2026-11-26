package dev.coilix.concurrency

import dev.coilix.config.Environment
import scala.concurrent.duration.DurationInt
import scala.concurrent.duration.FiniteDuration

/**
 * Settings of the cats-effect runtime of Coilix under `coilix.cats-effect`.
 *
 * ```yaml
 * coilix:
 *   cats-effect:
 *     shutdown-timeout: 1m
 *     exit-after-runners: true
 *     blocked-thread-detection: true
 * ```
 *
 * @param shutdownTimeout
 *   how long the context waits for the finalizer of each bean made by an effect when it closes, unless the `@Bean` method sets
 *   its own limit with [[dev.coilix.annotation.ShutdownTimeout]]
 * @param exitAfterRunners
 *   whether an application started by a `CoilixIOApp` of the boot module closes and ends after its runners, as a batch job,
 *   instead of running until it is cancelled
 * @param blockedThreadDetection
 *   whether the runtime of a `CoilixIOApp` warns when a compute thread blocks, which costs a little on every blocking call
 */
final case class CatsEffectSettings(
  shutdownTimeout:        FiniteDuration = 30.seconds,
  exitAfterRunners:       Boolean        = false,
  blockedThreadDetection: Boolean        = false
)

/** Reading of [[dev.coilix.concurrency.CatsEffectSettings]] from the configuration. */
object CatsEffectSettings {

  /** The configuration path of the settings. */
  val path: String = "coilix.cats-effect"

  /** The settings under [[path]] in `environment`, or the defaults when there are none. */
  def from(environment: Environment): CatsEffectSettings =
    environment.get[CatsEffectSettings](path).getOrElse(CatsEffectSettings())
}
