package dev.coilix.events

import cats.data.EitherT
import cats.effect.IO
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
 *   def place(order: Order): IO[Unit] =
 *     orders.save(order) *> events.publish(OrderPlaced(order.id, order.customer))
 * }
 * ```
 */
trait EventPublisher {

  /**
   * Delivers `event` to every listener of its type or of a supertype.
   *
   * Synchronous listeners run in the fiber of the caller before the effect ends. Asynchronous listeners start on fibers of
   * their own only after every synchronous listener succeeded. An event published before the context has started waits and is
   * delivered right after the start.
   *
   * The effect ends with `Right` when the event was delivered or kept for the start, or with a
   * [[dev.coilix.events.PublishError]] if the context is closing and the caller is not a listener, or the context is closed.
   * It fails with the error of the first synchronous listener that fails, with the errors of the further failing synchronous
   * listeners added as suppressed; every synchronous listener runs even when an earlier one fails.
   */
  def tryPublish[E: TypeInfo](event: E): EitherT[IO, PublishError, Unit]

  /**
   * Delivers `event` like [[dev.coilix.events.EventPublisher.tryPublish]], for callers that do not handle a closed context:
   * the effect fails with [[dev.coilix.events.EventBusClosedException]] instead, or with the error of the first synchronous
   * listener that fails.
   */
  final def publish[E: TypeInfo](event: E): IO[Unit] =
    tryPublish(event).foldF(error => IO.raiseError(error.exception), IO.pure)
}
