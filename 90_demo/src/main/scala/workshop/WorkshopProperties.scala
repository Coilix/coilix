package dev.coilix.demo
package workshop

import dev.coilix.annotation.ConfigurationProperties
import dev.coilix.demo.access.Mechanic
import dev.coilix.demo.access.MechanicId

/**
 * The workshop, bound from the `workshop` section of `application.yaml`.
 *
 * Shows binding of nested case classes and lists, value types such as [[dev.coilix.demo.vehicle.Vin]], enums written in
 * kebab or lower case, a [[dev.coilix.config.valuetype.Secret]] and a Scala default argument.
 */
@ConfigurationProperties("workshop")
final case class WorkshopProperties(
  name:      String,
  oemPortal: OemPortalSettings,
  mechanics: List[Mechanic],
  vehicles:  List[VehicleSettings]
) {

  /** The mechanic with the identifier `id`. */
  def mechanic(id: MechanicId): Option[Mechanic] = mechanics.find(_.id == id)
}
