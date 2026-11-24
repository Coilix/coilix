package dev.coilix.processing

/**
 * A processor whose work on a shard is a blocking loop on its own virtual thread.
 *
 * [[dev.coilix.processing.BlockingProcessor.run]] works until [[dev.coilix.processing.Shard.stopRequested]] is true and
 * then returns. When it returns while the shard is not stopping, the work is complete and `on-completion` decides what
 * happens next. When it throws, the restart policy restarts it. A shard that does not stop within half of the stop
 * timeout gets its thread interrupted:
 *
 * ```scala
 * @Processor
 * final class InvoiceExport(invoices: InvoiceRepository) extends BlockingProcessor {
 *   override val id: ShopProcessor = ShopProcessor.InvoiceExport
 *
 *   override def run(shard: Shard): Unit =
 *     if (!shard.stopRequested) {
 *       invoices.exportNext(shard.slots)
 *       run(shard)
 *     }
 * }
 * ```
 */
trait BlockingProcessor extends ProcessorDefinition {

  /** Works on the shard until it stops, on a thread named `coilix-processor-<name>-<shard>`. */
  def run(shard: Shard): Unit
}
