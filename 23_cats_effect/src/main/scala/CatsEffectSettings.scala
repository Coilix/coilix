package dev.coilix.catseffect

import dev.coilix.config.Environment
import scala.concurrent.duration.DurationInt
import scala.concurrent.duration.FiniteDuration

/**
 * Settings of the cats-effect module under `coilix.cats-effect`.
 *
 * ```yaml
 * coilix:
 *   cats-effect:
 *     shutdown-timeout: 1m
 * ```
 *
 * @param shutdownTimeout
 *   how long the context waits for the finalizer of each bean made by an effect when it closes, unless the `@Bean` method sets
 *   its own limit with [[dev.coilix.annotation.ShutdownTimeout]]
 */
final case class CatsEffectSettings(shutdownTimeout: FiniteDuration = 30.seconds)

/** Reading of [[dev.coilix.catseffect.CatsEffectSettings]] from the configuration. */
object CatsEffectSettings {

  /** The configuration path of the settings. */
  val path: String = "coilix.cats-effect"

  /** The settings under [[path]] in `environment`, or the defaults when there are none. */
  def from(environment: Environment): CatsEffectSettings =
    environment.get[CatsEffectSettings](path).getOrElse(CatsEffectSettings())
}
