package com.example
package processing

import dev.coilix.processing.ElementContext
import dev.coilix.processing.InputSource
import dev.coilix.processing.ProcessorId
import dev.coilix.processing.Shard
import dev.coilix.processing.StatelessProcessor
import dev.coilix.processing.annotation.Processor

@Processor
final class InvoiceMailer(orders: OrderStream, mail: SentMail) extends StatelessProcessor[Order] {

  override def id: ProcessorId = ShopProcessor.InvoiceMailer

  override def defaultShards: Int = 2

  override def source(shard: Shard): InputSource[Order] = orders.source(shard)

  override def process(order: Order, context: ElementContext): Unit =
    mail.emit(List(s"Invoice ${order.number} for ${order.customer}"))
}
