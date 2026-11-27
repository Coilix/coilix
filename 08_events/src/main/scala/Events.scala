package dev.coilix.events

import dev.coilix.common.TypeInfo

/**
 * Publishes events through the [[dev.coilix.events.EventPublisher]] in scope, for code that keeps the publisher as a `using`
 * parameter:
 *
 * ```scala
 * @Service
 * final class Checkout(using EventPublisher) {
 *   def place(order: Order): Unit = Events.publish(OrderPlaced(order.id, order.customer))
 * }
 * ```
 */
object Events {

  /**
   * Publishes `event` like [[dev.coilix.events.EventPublisher.publish]].
   *
   * @throws java.lang.Exception
   *   the exception of the first synchronous listener that fails
   * @throws dev.coilix.events.EventBusClosedException
   *   if the context is closing and the caller is not a listener, or the context is closed
   */
  def publish[E: TypeInfo](event: E)(using publisher: EventPublisher): Unit =
    publisher.publish(event)

  /**
   * Publishes `event` like [[dev.coilix.events.EventPublisher.tryPublish]], returning a closed context as a
   * [[dev.coilix.events.PublishError]].
   *
   * @throws java.lang.Exception
   *   the exception of the first synchronous listener that fails
   */
  def tryPublish[E: TypeInfo](event: E)(using publisher: EventPublisher): Either[PublishError, Unit] =
    publisher.tryPublish(event)
}
