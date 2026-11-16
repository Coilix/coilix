package dev.coilix.demo
package workshop

import dev.coilix.annotation.Component
import dev.coilix.annotation.Order
import dev.coilix.annotation.Slf4s
import dev.coilix.boot.ApplicationArguments
import dev.coilix.boot.ApplicationRunner
import dev.coilix.demo.access.AuthToken
import dev.coilix.demo.access.Mechanic
import dev.coilix.demo.access.MechanicId
import dev.coilix.demo.access.VehicleSystem
import dev.coilix.demo.vehicle.RequestId
import dev.coilix.demo.vehicle.Vehicle
import dev.coilix.demo.vehicle.VehicleRegistry
import dev.coilix.demo.vehicle.Vin
import java.time.Clock
import scala.util.Try

/**
 * Runs the inspection job: a mechanic asks for access, gets a signed token and works through the steps on the vehicle.
 *
 * `--mechanic=M-003` picks another mechanic and `--vin=...` another vehicle. Runs first because of `@Order(1)`. The
 * optional [[dev.coilix.demo.workshop.TelematicsUplink]] has no bean, so it is injected as `None`. The
 * [[dev.coilix.demo.workshop.Notifier]] depends on the active profile: `--coilix.profiles.active=dev` logs notifications.
 */
@Component
@Order(1)
@Slf4s
final class WorkshopShift(
  properties: WorkshopProperties,
  registry:   VehicleRegistry,
  system:     VehicleSystem,
  clock:      Clock,
  uplink:     Option[TelematicsUplink],
  notifier:   Notifier
) extends ApplicationRunner {

  override def run(arguments: ApplicationArguments): Unit =
    (mechanic(arguments), vehicle(arguments)) match {
      case (Some(mechanic), Some(vehicle)) => inspect(mechanic, vehicle)
      case _                               => log.warn("Unknown mechanic or vehicle, check --mechanic and --vin")
    }

  private def mechanic(arguments: ApplicationArguments): Option[Mechanic] =
    Try(MechanicId(arguments.optionValue("mechanic").getOrElse("M-001"))).toOption.flatMap(properties.mechanic)

  private def vehicle(arguments: ApplicationArguments): Option[Vehicle] =
    arguments.optionValue("vin").fold(registry.all.headOption)(vin => Try(Vin(vin)).toOption.flatMap(registry.find))

  private def inspect(mechanic: Mechanic, vehicle: Vehicle): Unit = {
    val steps = ServiceJob.inspection.filter(step => vehicle.modules.contains(step.module))
    log.info(
      s"${properties.name}: ${mechanic.name} (${mechanic.certification}) starts an inspection of ${vehicle.model} ${vehicle.vin.value}"
    )
    system.authorize(
      mechanic,
      vehicle,
      steps.map(_.grant).toSet,
      clock.instant()
    ).fold(error => log.warn(s"Access refused: $error"), token => work(vehicle, token, steps))
    uplink.fold(
      log.info("No telematics uplink, the job report stays in the workshop")
    )(_.send(s"Inspection of ${vehicle.vin.value} done"))
    log.info(s"Notifications: ${notifier.description}")
    notifier.send(s"${mechanic.name} finished the inspection of ${vehicle.vin.value}")
  }

  private def work(vehicle: Vehicle, token: AuthToken, steps: List[ServiceStep]): Unit = {
    log.info(s"Session ${token.claims.session.value} valid until ${token.claims.expiresAt}")
    steps.zipWithIndex.foreach((step, index) => perform(vehicle, token, step, RequestId(s"R-${index + 1}")))
  }

  private def perform(vehicle: Vehicle, token: AuthToken, step: ServiceStep, requestId: RequestId): Unit =
    vehicle
      .program(token, step.module, step.command(requestId), clock.instant())
      .fold(
        error  => log.warn(s"${step.description}: refused with $error"),
        result => log.info(s"${step.description}: ${result.describe}")
      )
}
