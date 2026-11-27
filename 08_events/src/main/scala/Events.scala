package dev.coilix.events

import cats.data.EitherT
import cats.effect.IO
import dev.coilix.common.TypeInfo

/**
 * Publishes events through the [[dev.coilix.events.EventPublisher]] in scope, for code that keeps the publisher as a `using`
 * parameter:
 *
 * ```scala
 * @Service
 * final class Checkout(using EventPublisher) {
 *   def place(order: Order): IO[Unit] = Events.publish(OrderPlaced(order.id, order.customer))
 * }
 * ```
 */
object Events {

  /**
   * Publishes `event` like [[dev.coilix.events.EventPublisher.publish]]: the effect fails with
   * [[dev.coilix.events.EventBusClosedException]] when the context no longer takes events, or with the error of the first
   * synchronous listener that fails.
   */
  def publish[E: TypeInfo](event: E)(using publisher: EventPublisher): IO[Unit] =
    publisher.publish(event)

  /**
   * Publishes `event` like [[dev.coilix.events.EventPublisher.tryPublish]], returning a closed context as a
   * [[dev.coilix.events.PublishError]]. The effect fails with the error of the first synchronous listener that fails.
   */
  def tryPublish[E: TypeInfo](event: E)(using publisher: EventPublisher): EitherT[IO, PublishError, Unit] =
    publisher.tryPublish(event)
}
