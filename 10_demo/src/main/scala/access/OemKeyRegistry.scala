package dev.coilix.demo
package access

import dev.coilix.annotation.PostConstruct
import dev.coilix.annotation.Repository
import dev.coilix.annotation.Slf4s
import dev.coilix.demo.vehicle.Vehicle
import dev.coilix.demo.vehicle.Vin
import dev.coilix.demo.workshop.WorkshopProperties
import java.security.KeyPair

/**
 * Trusted keys as registered with the OEM portal: every configured vehicle trusts the key of this workshop.
 *
 * After creation it logs the portal address and API key. The key is a [[dev.coilix.config.valuetype.Secret]], so the log
 * shows `*****`.
 */
@Repository
@Slf4s
final class OemKeyRegistry(properties: WorkshopProperties, workshopKeys: KeyPair) extends PublicKeyRepository {
  private val trusted: Map[Vin, PublicKey] = properties.vehicles.map(_.vin -> PublicKey(workshopKeys.getPublic)).toMap

  override def getKey(vehicle: Vehicle): Either[AccessError, PublicKey] =
    trusted.get(vehicle.vin).toRight(AccessError.UnknownVehicle)

  @PostConstruct
  private def synchronize(): Unit =
    log.info(
      s"Registered the workshop key for ${trusted.size} vehicles at ${properties.oemPortal.url} with API key ${properties.oemPortal.apiKey}"
    )
}
