package com.example
package processing

import cats.effect.IO
import cats.syntax.all.*
import dev.coilix.processing.IOProcessor
import dev.coilix.processing.Shard
import dev.coilix.processing.ShardKeys
import dev.coilix.processing.annotation.Processor

@Processor
final class InvoiceExport(invoices: Invoices) extends IOProcessor {
  override val id: ShopProcessor = ShopProcessor.InvoiceExport

  override def defaultShards: Int = 2

  override def run(shard: Shard): IO[Unit] =
    invoices.customers
      .filter(customer => shard.owns(ShardKeys.slotOf(customer, shard.slots.slotCount)))
      .traverse_(customer => IO.blocking(invoices.send(customer)))
}
