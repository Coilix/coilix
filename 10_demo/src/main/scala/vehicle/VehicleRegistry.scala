package dev.coilix.demo
package vehicle

import dev.coilix.annotation.Repository
import dev.coilix.demo.access.PublicKeyRepository
import dev.coilix.demo.access.ReplayGuard
import dev.coilix.demo.access.TokenVerifier
import dev.coilix.demo.workshop.ModuleSettings
import dev.coilix.demo.workshop.VehicleSettings
import dev.coilix.demo.workshop.WorkshopProperties

/**
 * The vehicles in the workshop, created from the configuration.
 *
 * Receives every [[dev.coilix.demo.vehicle.ModuleDriver]] bean as a list and connects each configured module to the
 * driver for its kind.
 */
@Repository
final class VehicleRegistry(
  properties:  WorkshopProperties,
  drivers:     List[ModuleDriver],
  keys:        PublicKeyRepository,
  verifier:    TokenVerifier,
  replayGuard: ReplayGuard
) {
  private val vehicles: List[Vehicle] = properties.vehicles.map(connect)

  /** All vehicles in configuration order. */
  def all: List[Vehicle] = vehicles

  /** The vehicle with the identification number `vin`. */
  def find(vin: Vin): Option[Vehicle] = vehicles.find(_.vin == vin)

  private def connect(settings: VehicleSettings): Vehicle =
    ConnectedVehicle(settings.vin, settings.model, settings.modules.map(module).toMap, keys, verifier, replayGuard)

  private def module(settings: ModuleSettings): (ModuleId, Module) =
    settings.id -> DriverModule(settings, driverFor(settings.kind))

  private def driverFor(kind: ModuleKind): ModuleDriver =
    drivers.find(_.kind == kind).getOrElse(throw IllegalStateException(s"No module driver for $kind"))
}
