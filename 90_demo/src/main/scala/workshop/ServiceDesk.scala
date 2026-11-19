package dev.coilix.demo
package workshop

import dev.coilix.annotation.Component
import dev.coilix.annotation.Order
import dev.coilix.annotation.Slf4s
import dev.coilix.boot.ApplicationArguments
import dev.coilix.boot.ApplicationRunner
import dev.coilix.common.edit.EditOption
import dev.coilix.demo.diagnostics.FleetScan
import dev.coilix.demo.vehicle.Vehicle
import dev.coilix.demo.vehicle.VehicleRegistry
import scala.concurrent.Await
import scala.concurrent.duration.DurationInt

/**
 * The front desk at the end of the shift: quotes a part, shows the job history, updates the job notes, archives the report and
 * scans the fleet. Runs last because of `@Order(4)`.
 *
 * It shows `@Qualifier` ([[dev.coilix.demo.workshop.PartsQuote]]), beans told apart by type arguments (`JobLog[Option]` and
 * `JobLog[List]`), `EditOption`, an `@Async` bean ([[dev.coilix.demo.workshop.JobArchive]]) and a bean of an optional module
 * that is injected as `Option`, because the module can be disabled.
 */
@Component
@Order(4)
@Slf4s
final class ServiceDesk(
  quote:    PartsQuote,
  latest:   JobLog[Option],
  history:  JobLog[List],
  archive:  JobArchive,
  fleet:    Option[FleetScan],
  registry: VehicleRegistry
) extends ApplicationRunner {

  override def run(arguments: ApplicationArguments): Unit = {
    log.info(s"Parts quote, ${quote.compare("brake pads")}")
    registry.all.headOption.foreach(showHistory)
    log.info(s"Job notes: ${updatedNotes}")
    log.info(Await.result(archive.archive("shift report"), 10.seconds))
    fleet.fold(log.info("Diagnostics module is disabled"))(_.scan().foreach(line => log.info(s"Fleet scan: $line")))
  }

  /** The notes after the customer note is set and the internal note is cleared. */
  def updatedNotes: JobNotes =
    JobNotesChange(customer = EditOption.SetTo(Some("Brake pads replaced")), internal = EditOption.SetTo(None))
      .applyTo(JobNotes(None, Some("check brake pads")))

  private def showHistory(vehicle: Vehicle): Unit =
    log.info(
      s"Jobs of ${vehicle.vin.value}: latest ${latest.jobs(vehicle.vin).getOrElse("none")}, all ${history.jobs(vehicle.vin).mkString(", ")}"
    )
}
