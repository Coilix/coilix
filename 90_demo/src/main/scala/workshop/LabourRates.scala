package dev.coilix.demo
package workshop

import cats.effect.Sync
import dev.coilix.annotation.Component

/**
 * The hourly rates of the workshop, written in tagless final style: the container binds `F` to `IO`, so other beans inject
 * `LabourRates[IO]`, and the `Sync[F]` context bound receives the `Async[IO]` bean of the cats-effect module.
 */
@Component
final class LabourRates[F[_]: Sync] {
  private val hourly = Map("brakes" -> 90, "engine" -> 120)

  /** The hourly rate for work on `system`, or the general rate. */
  def hourlyRate(system: String): F[Int] = Sync[F].delay(hourly.getOrElse(system, 80))
}
