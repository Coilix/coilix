package dev.coilix.processing

import dev.coilix.config.Environment
import scala.concurrent.duration.FiniteDuration

/**
 * The keys under `restart` of a processor or of `coilix.processors.defaults`, each optional.
 *
 * ```yaml
 * restart:
 *   backoff: { initial: 1s, max: 2m, multiplier: 2.0, jitter: 0.2 }
 *   max-attempts: 10
 *   within: 15m
 *   reset-after: 5m
 * ```
 */
final case class RestartSettings(
  initial:     Option[FiniteDuration] = None,
  max:         Option[FiniteDuration] = None,
  multiplier:  Option[Double]         = None,
  jitter:      Option[Double]         = None,
  maxAttempts: Option[Int]            = None,
  within:      Option[FiniteDuration] = None,
  resetAfter:  Option[FiniteDuration] = None
) {

  /** These settings with each missing key taken from `fallback`. */
  def orElse(fallback: RestartSettings): RestartSettings =
    RestartSettings(
      initial.orElse(fallback.initial),
      max.orElse(fallback.max),
      multiplier.orElse(fallback.multiplier),
      jitter.orElse(fallback.jitter),
      maxAttempts.orElse(fallback.maxAttempts),
      within.orElse(fallback.within),
      resetAfter.orElse(fallback.resetAfter)
    )

  /** The restart policy with the defaults of Coilix for the missing keys. */
  def policy: RestartPolicy = {
    val defaults = RestartPolicy()
    RestartPolicy(
      Backoff(
        initial.getOrElse(defaults.backoff.initial),
        max.getOrElse(defaults.backoff.max),
        multiplier.getOrElse(defaults.backoff.multiplier),
        jitter.getOrElse(defaults.backoff.jitter)
      ),
      maxAttempts.getOrElse(defaults.maxAttempts),
      within.getOrElse(defaults.within),
      resetAfter.getOrElse(defaults.resetAfter)
    )
  }
}

/** Reads restart settings. */
object RestartSettings {

  /** Reads the keys under `path`. */
  def read(environment: Environment, path: String): RestartSettings =
    RestartSettings(
      initial     = environment.get[FiniteDuration](s"$path.backoff.initial"),
      max         = environment.get[FiniteDuration](s"$path.backoff.max"),
      multiplier  = environment.get[Double](s"$path.backoff.multiplier"),
      jitter      = environment.get[Double](s"$path.backoff.jitter"),
      maxAttempts = environment.get[Int](s"$path.max-attempts"),
      within      = environment.get[FiniteDuration](s"$path.within"),
      resetAfter  = environment.get[FiniteDuration](s"$path.reset-after")
    )
}
