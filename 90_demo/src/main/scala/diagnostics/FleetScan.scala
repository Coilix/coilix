package dev.coilix.demo
package diagnostics

import dev.coilix.annotation.Component
import dev.coilix.concurrency.TaskExecutor
import dev.coilix.demo.vehicle.Vehicle
import dev.coilix.demo.vehicle.VehicleRegistry
import scala.concurrent.Await
import scala.concurrent.ExecutionContext
import scala.concurrent.Future
import scala.concurrent.duration.DurationInt

/**
 * Checks every vehicle of the workshop at the same time on the Coilix [[dev.coilix.concurrency.TaskExecutor]].
 *
 * It is added by [[dev.coilix.demo.diagnostics.DiagnosticsModule]], not found by scanning.
 */
@Component
final class FleetScan(registry: VehicleRegistry, executor: TaskExecutor) {

  /** One line per vehicle, with the thread that checked it. */
  def scan(): List[String] = {
    given ExecutionContext = executor.executionContext
    Await.result(Future.sequence(registry.all.map(check)), 10.seconds)
  }

  private def check(vehicle: Vehicle): Future[String] =
    executor.submit(s"${vehicle.vin.value}: ${vehicle.modules.size} modules respond (${Thread.currentThread.getName})")
}
