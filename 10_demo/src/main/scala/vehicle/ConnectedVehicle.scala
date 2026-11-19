package dev.coilix.demo
package vehicle

import dev.coilix.demo.access.PublicKeyRepository
import dev.coilix.demo.access.ReplayGuard
import dev.coilix.demo.access.TokenVerifier

/** A vehicle connected to the workshop diagnostic tester. [[dev.coilix.demo.vehicle.VehicleRegistry]] creates one per configured vehicle. */
final class ConnectedVehicle(
  val vin:                   Vin,
  val model:                 String,
  val modules:               Map[ModuleId, Module],
  protected val keys:        PublicKeyRepository,
  protected val verifier:    TokenVerifier,
  protected val replayGuard: ReplayGuard
) extends Vehicle
