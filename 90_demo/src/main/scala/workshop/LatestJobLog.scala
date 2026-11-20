package dev.coilix.demo
package workshop

import dev.coilix.annotation.Component
import dev.coilix.demo.vehicle.Vin

/** The latest job of a vehicle, injected wherever a `JobLog[Option]` is required. */
@Component
final class LatestJobLog extends JobLog[Option] {

  override def jobs(vin: Vin): Option[String] = Some("inspection 2026-11")
}
