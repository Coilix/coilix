package dev.coilix.demo
package workshop

import dev.coilix.annotation.Service
import dev.coilix.demo.vehicle.VehicleRegistry
import org.apache.pekko.stream.Materializer
import org.apache.pekko.stream.scaladsl.Sink
import org.apache.pekko.stream.scaladsl.Source
import scala.concurrent.Future

/**
 * Counts the control modules of the whole fleet with a Pekko stream, to plan which testers the workshop needs.
 *
 * The `Materializer` comes from the pekko module as a `using` parameter, and the stream runs on the task executor of the
 * workshop, the compute pool of cats-effect.
 */
@Service
final class ModuleCensus(registry: VehicleRegistry)(using Materializer) {

  /** The number of modules of each identifier in the fleet, such as `engine -> 2`. */
  def count(): Future[Map[String, Int]] =
    Source(registry.all)
      .mapConcat(_.modules.keys.map(_.value).toList)
      .runWith(Sink.fold(Map.empty[String, Int])((counts, id) => counts.updated(id, counts.getOrElse(id, 0) + 1)))
}
