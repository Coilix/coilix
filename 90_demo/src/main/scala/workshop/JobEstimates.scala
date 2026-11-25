package dev.coilix.demo
package workshop

import cats.effect.Sync
import cats.syntax.functor.*
import dev.coilix.annotation.Component

/** Estimates the labour cost of a job, in tagless final style on top of [[dev.coilix.demo.workshop.LabourRates]]. */
@Component
final class JobEstimates[F[_]: Sync](rates: LabourRates[F]) {

  /** The labour cost of `hours` of work on `system`, such as `brakes: 3 h x 90 = 270`. */
  def estimate(system: String, hours: Int): F[String] =
    rates.hourlyRate(system).map(rate => s"$system: $hours h x $rate = ${hours * rate}")
}
