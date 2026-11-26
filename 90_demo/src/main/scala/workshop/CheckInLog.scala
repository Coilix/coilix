package dev.coilix.demo
package workshop

import dev.coilix.annotation.Component
import dev.coilix.demo.vehicle.Vin
import java.util.concurrent.atomic.AtomicReference

/**
 * The vehicles that arrived at [[dev.coilix.demo.workshop.VehicleCheckIn]] and the batches it saved, in order, safe to write
 * from its shard.
 */
@Component
final class CheckInLog {

  private val arrived = AtomicReference(List.empty[Vin])

  private val saved = AtomicReference(List.empty[List[Vin]])

  /** Notes a vehicle that arrived and waits in the current batch. */
  def arrive(vin: Vin): Unit = arrived.getAndUpdate(_ :+ vin): Unit

  /** Every vehicle that arrived, oldest first. */
  def arrivals: List[Vin] = arrived.get

  /** Saves one batch of checked-in vehicles. */
  def save(batch: List[Vin]): Unit = saved.getAndUpdate(_ :+ batch): Unit

  /** Every saved batch, oldest first. */
  def batches: List[List[Vin]] = saved.get
}
