package com.example
package processing

import dev.coilix.processing.ProcessorId

sealed trait ShopProcessor extends ProcessorId

object ShopProcessor {
  case object InvoiceExport extends ShopProcessor

  case object OrderWatch extends ShopProcessor

  case object InvoiceMailer extends ShopProcessor
}
