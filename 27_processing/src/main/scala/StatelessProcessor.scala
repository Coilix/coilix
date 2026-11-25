package dev.coilix.processing

import scala.concurrent.duration.DurationInt
import scala.concurrent.duration.FiniteDuration

/**
 * A processor that handles each element on its own, without state between elements.
 *
 * The worker of each shard reads the [[source]] of the shard, calls [[process]] for each element in order and acknowledges the
 * element when `process` returns. When `process` throws, the worker fails with an
 * [[dev.coilix.processing.ElementFailedException]] and is restarted by the restart policy of the processor; the element was not
 * acknowledged, so the source delivers it again with a higher `delivery`. The worker counts processed and failed elements and
 * the processing time in the [[dev.coilix.processing.ShardMetrics]] of the shard.
 *
 * ```scala
 * @Processor
 * final class InvoiceMailer(orders: OrderStream, mail: MailSender) extends StatelessProcessor[Order] {
 *   override def id: ProcessorId = ShopProcessor.InvoiceMailer
 *
 *   override def source(shard: Shard): InputSource[Order] = orders.source(shard)
 *
 *   override def process(order: Order, context: ElementContext): Unit = mail.send(order.customer, s"Invoice ${order.number}")
 * }
 * ```
 */
trait StatelessProcessor[T] extends ProcessorDefinition {

  /** The source of the elements of `shard`, created each time the worker of the shard starts. */
  def source(shard: Shard): InputSource[T]

  /** Handles one element. An exception restarts the worker, and the element comes again. */
  def process(element: T, context: ElementContext): Unit

  /** How many elements the worker asks the source for at once. */
  def pollSize: Int = 100

  /** How long the worker waits for elements in one call to the source before it checks whether to stop. */
  def pollTimeout: FiniteDuration = 1.second
}
