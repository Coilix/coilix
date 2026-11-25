package dev.coilix.processing
package stateless

import dev.coilix.annotation.Value
import dev.coilix.processing.annotation.Processor
import scala.concurrent.duration.DurationInt
import scala.concurrent.duration.FiniteDuration

/** Ships every order, and fails on the first delivery of order `shipping.jammed`. */
@Processor
final class Shipping(orders: Orders, shipped: Shipped, @Value("${shipping.jammed:0}") jammed: Int)
    extends StatelessProcessor[Order] {

  override val id: TestProcessor = TestProcessor.Shipping

  override def defaultShards: Int = 2

  override def pollTimeout: FiniteDuration = 50.millis

  override def source(shard: Shard): InputSource[Order] = orders.source(shard)

  override def process(order: Order, context: ElementContext): Unit =
    if (order.number == jammed && context.delivery == 1) throw IllegalStateException("the label printer jammed")
    else shipped.emit(List(order.number))
}
