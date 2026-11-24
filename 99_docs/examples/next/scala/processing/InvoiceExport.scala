package com.example
package processing

import dev.coilix.processing.BlockingProcessor
import dev.coilix.processing.Shard
import dev.coilix.processing.ShardKeys
import dev.coilix.processing.annotation.Processor

@Processor
final class InvoiceExport(invoices: Invoices) extends BlockingProcessor {

  override val id: ShopProcessor = ShopProcessor.InvoiceExport

  override def defaultShards: Int = 2

  override def run(shard: Shard): Unit =
    invoices.customers
      .filter(customer => shard.owns(ShardKeys.slotOf(customer, shard.slots.slotCount)))
      .foreach(invoices.send)
}
