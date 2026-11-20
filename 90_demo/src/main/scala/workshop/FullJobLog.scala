package dev.coilix.demo
package workshop

import dev.coilix.annotation.Component
import dev.coilix.demo.vehicle.Vin

/** Every job of a vehicle, injected wherever a `JobLog[List]` is required. */
@Component
final class FullJobLog extends JobLog[List] {

  override def jobs(vin: Vin): List[String] = List("oil change 2026-03", "inspection 2026-11")
}
