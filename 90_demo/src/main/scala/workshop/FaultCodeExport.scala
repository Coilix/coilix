package dev.coilix.demo
package workshop

import dev.coilix.annotation.Slf4s
import dev.coilix.processing.BlockingProcessor
import dev.coilix.processing.Shard
import dev.coilix.processing.ShardKeys
import dev.coilix.processing.annotation.Processor

/**
 * Exports the fault codes of the fleet to the [[dev.coilix.demo.workshop.FaultLedger]] once per start of the workshop.
 *
 * A background processor, not a bean: the `ProcessorManager` creates it and runs its two shards on their own virtual
 * threads, `coilix-processor-fault-code-export-0` and `-1`. Each vehicle belongs to the slot of its VIN, so each shard
 * exports its own vehicles, and they stay in the same slots when `coilix.processors.fault-code-export.shards` changes.
 * The work ends after one pass, so the shards become `Completed`.
 */
@Processor
@Slf4s
final class FaultCodeExport(properties: WorkshopProperties, ledger: FaultLedger) extends BlockingProcessor {

  override val id: WorkshopProcessor = WorkshopProcessor.FaultCodeExport

  override def defaultShards: Int = 2

  override def run(shard: Shard): Unit = {
    val vehicles = properties.vehicles.filter(vehicle => shard.owns(ShardKeys.slotOf(vehicle.vin.value, shard.slots.slotCount)))
    vehicles.foreach(vehicle => ledger.record(vehicle.vin, vehicle.modules.flatMap(_.faults)))
    log.info(s"Shard $shard on ${Thread.currentThread.getName} exported the fault codes of ${vehicles.size} vehicles")
  }
}
