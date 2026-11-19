package dev.coilix.demo
package workshop

import dev.coilix.annotation.Component
import dev.coilix.annotation.Order
import dev.coilix.annotation.Slf4s
import dev.coilix.annotation.Value
import dev.coilix.boot.ApplicationArguments
import dev.coilix.boot.ApplicationRunner
import dev.coilix.demo.access.Certification
import dev.coilix.demo.access.Grant
import dev.coilix.demo.access.Mechanic
import dev.coilix.demo.access.MechanicId
import dev.coilix.demo.access.ReadOnlyAccessPolicy
import dev.coilix.demo.access.TokenIssuer
import dev.coilix.demo.access.WorkshopAccessSystem
import dev.coilix.demo.vehicle.Action
import dev.coilix.demo.vehicle.ModuleCommand
import dev.coilix.demo.vehicle.ModuleId
import dev.coilix.demo.vehicle.RequestId
import dev.coilix.demo.vehicle.Vehicle
import dev.coilix.demo.vehicle.VehicleRegistry
import java.time.Clock
import scala.concurrent.duration.FiniteDuration

/**
 * The app a vehicle owner uses to read fault codes at home.
 *
 * Injects [[dev.coilix.demo.access.ReadOnlyAccessPolicy]] by its class instead of the primary policy and builds its own
 * [[dev.coilix.demo.access.WorkshopAccessSystem]] with it. Runs last because of `@Order(3)`.
 */
@Component
@Order(3)
@Slf4s
final class OwnerApp(
  registry:                                    VehicleRegistry,
  policy:                                      ReadOnlyAccessPolicy,
  issuer:                                      TokenIssuer,
  clock:                                       Clock,
  @Value("${workshop.token-ttl:5m}") tokenTtl: FiniteDuration
) extends ApplicationRunner {
  private val owner  = Mechanic(MechanicId("M-900"), "Owner app", Certification.Apprentice)
  private val access = WorkshopAccessSystem(policy, issuer, tokenTtl, 2)
  private val engine = ModuleId("engine")

  override def run(arguments: ApplicationArguments): Unit =
    registry.all.headOption.fold(log.warn("The owner app has no vehicle"))(useApp)

  private def useApp(vehicle: Vehicle): Unit = {
    access
      .authorize(owner, vehicle, Set(Grant(engine, Action.ReadFaults)), clock.instant())
      .flatMap(vehicle.program(_, engine, ModuleCommand(Action.ReadFaults, RequestId("O-1")), clock.instant()))
      .fold(
        error     => log.warn(s"Owner app cannot read faults: $error"),
        result    => log.info(s"Owner app reads engine faults: ${result.describe}")
      )
    access
      .authorize(owner, vehicle, Set(Grant(engine, Action.Code)), clock.instant())
      .fold(error => log.info(s"Owner app may not code the engine: $error"), _ => log.error("Owner app got a coding grant"))
  }
}
