package dev.coilix.demo
package workshop

import cats.effect.IO
import cats.syntax.all.*
import dev.coilix.annotation.Component
import dev.coilix.annotation.Order
import dev.coilix.annotation.Slf4s
import dev.coilix.boot.ApplicationArguments
import dev.coilix.boot.IOApplicationRunner
import dev.coilix.common.edit.EditOption
import dev.coilix.context.Provider
import dev.coilix.demo.diagnostics.FleetScan
import dev.coilix.demo.vehicle.Vehicle
import dev.coilix.demo.vehicle.VehicleRegistry
import dev.coilix.processing.ProcessorManager

/**
 * The front desk at the end of the shift: quotes a part, shows the job history, updates the job notes, archives the report and
 * scans the fleet. Runs last because of `@Order(4)`.
 *
 * It shows `@Qualifier` ([[dev.coilix.demo.workshop.PartsQuote]]), beans told apart by type arguments (`JobLog[Option]` and
 * `JobLog[List]`), `EditOption`, an `@Async` bean ([[dev.coilix.demo.workshop.JobArchive]]) and a bean of an optional module
 * that is injected as `Option`, because the module can be disabled. The `@Lazy` [[dev.coilix.demo.workshop.WarrantyRegistry]]
 * comes through a `Provider`, so it is loaded only here. The [[dev.coilix.demo.workshop.ModuleCensus]] counts the modules of
 * the fleet with an fs2 stream. The background processors are reached only through the `ProcessorManager`. It is an
 * `IOApplicationRunner`, so the `Future` of the archive and the `IO` of the other services run on the fiber of the start.
 */
@Component
@Order(4)
@Slf4s
final class ServiceDesk(
  quote:      PartsQuote,
  latest:     JobLog[Option],
  history:    JobLog[List],
  archive:    JobArchive,
  fleet:      Option[FleetScan],
  registry:   VehicleRegistry,
  warranties: Provider[WarrantyRegistry],
  recalls:    RecallCheck,
  census:     ModuleCensus,
  processors: ProcessorManager
) extends IOApplicationRunner {

  override def run(arguments: ApplicationArguments): IO[Unit] =
    IO.blocking(deskReport()) *>
      IO.fromFuture(IO(archive.archive("shift report"))).flatMap(line => logF.info(line)) *>
      recalls.openRecalls.flatMap(_.traverse_(line => logF.info(s"Recalls: $line"))) *>
      census.count.flatMap(counts => logF.info(s"Modules in the fleet: ${described(counts)}")) *>
      IO.blocking(fleet.fold(log.info("Diagnostics module is disabled"))(_.scan().foreach(line =>
        log.info(s"Fleet scan: $line")
      ))) *>
      exportShards.flatMap(shards => logF.info(s"Fault code export: $shards"))

  private def deskReport(): Unit = {
    log.info(s"Parts quote, ${quote.compare("brake pads")}")
    registry.all.headOption.foreach(showHistory)
    registry.all.headOption.foreach(vehicle => log.info(s"Warranty: ${warranties.get().coverage(vehicle.vin)}"))
    log.info(s"Job notes: ${updatedNotes}")
  }

  private def described(counts: Map[String, Int]): String =
    counts.toList.sorted.map((id, count) => s"$count x $id").mkString(", ")

  /**
   * The state of each shard of the fault code export on this replica, such as `0 Completed, 1 Running`, or the reason why the
   * manager has no such processor.
   */
  def exportShards: IO[String] =
    processors
      .status(WorkshopProcessor.FaultCodeExport)
      .fold(_.message, _.shards.map(shard => s"${shard.index} ${shard.state}").mkString(", "))

  /** The notes after the customer note is set and the internal note is cleared. */
  def updatedNotes: JobNotes =
    JobNotesChange(customer = EditOption.SetTo(Some("Brake pads replaced")), internal = EditOption.SetTo(None))
      .applyTo(JobNotes(None, Some("check brake pads")))

  private def showHistory(vehicle: Vehicle): Unit =
    log.info(
      s"Jobs of ${vehicle.vin.value}: latest ${latest.jobs(vehicle.vin).getOrElse("none")}, all ${history.jobs(vehicle.vin).mkString(", ")}"
    )
}
