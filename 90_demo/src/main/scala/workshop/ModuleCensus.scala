package dev.coilix.demo
package workshop

import cats.effect.IO
import dev.coilix.annotation.Service
import dev.coilix.demo.vehicle.VehicleRegistry
import fs2.Stream

/**
 * Counts the control modules of the whole fleet with an fs2 stream, to plan which testers the workshop needs.
 *
 * The stream runs in `IO` on the compute pool of the workshop, which belongs to the cats-effect runtime of Coilix.
 */
@Service
final class ModuleCensus(registry: VehicleRegistry) {

  /** The number of modules of each identifier in the fleet, such as `engine -> 2`. */
  def count: IO[Map[String, Int]] =
    Stream
      .emits(registry.all)
      .covary[IO]
      .flatMap(vehicle => Stream.emits(vehicle.modules.keys.map(_.value).toList))
      .compile
      .fold(Map.empty[String, Int])((counts, id) => counts.updated(id, counts.getOrElse(id, 0) + 1))
}
