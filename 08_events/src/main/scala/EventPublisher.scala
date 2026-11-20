package dev.coilix.events

import dev.coilix.common.TypeInfo

/**
 * Publishes application events to the beans that listen for them, so the publisher does not depend on its listeners.
 *
 * The events module registers an `eventBus` bean that implements this trait. Events live only in the memory of the process:
 * they do not survive a restart and do not reach other replicas.
 *
 * ```scala
 * @Service
 * final class Checkout(events: EventPublisher) {
 *   def place(order: Order): Unit = {
 *     orders.save(order)
 *     events.publish(OrderPlaced(order.id, order.customer))
 *   }
 * }
 * ```
 */
trait EventPublisher {

  /**
   * Delivers `event` to every listener of its type or of a supertype.
   *
   * Synchronous listeners run in the calling thread before this method returns. Asynchronous listeners start on the task
   * executor only after every synchronous listener succeeded. An event published before the context has started waits and is
   * delivered right after the start.
   *
   * @throws java.lang.Exception
   *   the exception of the first synchronous listener that fails, with the exceptions of the further failing synchronous
   *   listeners added as suppressed; every synchronous listener runs even when an earlier one fails
   * @throws dev.coilix.events.EventBusClosedException
   *   if the context is closing and the caller is not a listener, or the context is closed
   */
  def publish[E: TypeInfo](event: E): Unit
}
