package dev.coilix.demo
package workshop

import cats.effect.IO
import cats.effect.unsafe.IORuntime
import cats.syntax.all.*
import dev.coilix.annotation.Service
import dev.coilix.concurrency.BlockingExecutor
import dev.coilix.concurrency.CoilixIO.*
import dev.coilix.demo.vehicle.Vehicle
import dev.coilix.demo.vehicle.VehicleRegistry

/**
 * Asks the manufacturer portal about open recalls of every vehicle at once, written with cats-effect `IO`.
 *
 * The `IORuntime` comes from the concurrency module, whose compute pool is the only compute pool of the workshop. Each portal
 * call waits for the network, so it runs on the [[dev.coilix.concurrency.BlockingExecutor]] through `IO.onBlockingExecutor`. The
 * [[dev.coilix.demo.workshop.RecallBulletin]] comes from an `IO` bean of [[dev.coilix.demo.workshop.PortalConfiguration]].
 */
@Service
final class RecallCheck(registry: VehicleRegistry, bulletin: RecallBulletin, runtime: IORuntime, blocking: BlockingExecutor) {
  private given IORuntime = runtime

  private given BlockingExecutor = blocking

  /** One line per vehicle with its open recalls and the thread that asked the portal. */
  def openRecalls(): List[String] =
    registry.all.parTraverse(vehicle => IO.onBlockingExecutor(askPortal(vehicle))).unsafeRunSync()

  private def askPortal(vehicle: Vehicle): String =
    s"${vehicle.vin.value}: no open recalls in bulletin ${bulletin.issue} (asked on ${Thread.currentThread.getName})"
}
