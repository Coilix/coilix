package dev.coilix.demo
package workshop

import dev.coilix.annotation.Lazy
import dev.coilix.annotation.Service
import dev.coilix.annotation.Slf4s
import dev.coilix.demo.vehicle.Vin

/**
 * Warranty records of the manufacturers, which take a while to load and are needed only at the front desk.
 *
 * It is `@Lazy`, so it is loaded when [[dev.coilix.demo.workshop.ServiceDesk]] first asks through its
 * `Provider[WarrantyRegistry]`, not when the workshop opens.
 */
@Service
@Lazy
@Slf4s
final class WarrantyRegistry {
  log.info("Loading warranty records of the manufacturers")

  /** The warranty of the vehicle, by its year of manufacture in the VIN. */
  def coverage(vin: Vin): String =
    if (vin.value.charAt(9) >= 'K') s"${vin.value} is under warranty" else s"${vin.value} is out of warranty"
}
