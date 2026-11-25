package dev.coilix.processing

import cats.effect.IO

/**
 * A processor whose work on a shard is an `IO`, run on its own fiber.
 *
 * When [[dev.coilix.processing.IOProcessor.run]] ends while the shard is not stopping, the work is complete and
 * `on-completion` decides what happens next. When it fails, the restart policy restarts it. Stopping the shard completes
 * [[dev.coilix.processing.Shard.stopped]], waits half of the stop timeout for the work to end and then cancels it, so work
 * that finishes its current step on [[dev.coilix.processing.Shard.stopRequested]] ends cleanly, and other work is
 * cancelled. Blocking calls go through `IO.blocking` or `IO.onBlockingExecutor`:
 *
 * ```scala
 * @Processor
 * final class InvoiceExport(invoices: InvoiceRepository) extends IOProcessor {
 *   override val id: ShopProcessor = ShopProcessor.InvoiceExport
 *
 *   override def run(shard: Shard): IO[Unit] =
 *     shard.stopRequested.ifM(IO.unit, invoices.exportNext(shard.slots) *> run(shard))
 * }
 * ```
 */
trait IOProcessor extends ProcessorDefinition {

  /** Works on the shard until it stops. */
  def run(shard: Shard): IO[Unit]
}
