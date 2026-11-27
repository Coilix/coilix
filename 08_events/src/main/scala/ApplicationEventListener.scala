package dev.coilix.events

import cats.effect.IO

/**
 * A bean that receives the application events of type `E` and of its subtypes, an alternative to a method annotated with
 * [[dev.coilix.annotation.EventListener]].
 *
 * The type argument must be known from the class of the bean, so the bean extends the trait with a concrete type, as a class or
 * an anonymous class. The listener is named `<bean>.onEvent` in log messages and in the configuration of retries.
 *
 * ```scala
 * @Component
 * final class OrderStatistics extends ApplicationEventListener[OrderPlaced] {
 *   private val placed = AtomicInteger()
 *
 *   override def onEvent(event: OrderPlaced): IO[Unit] = IO(placed.incrementAndGet()).void
 * }
 * ```
 *
 * @tparam E
 *   the type of the events
 */
trait ApplicationEventListener[E] {

  /** Handles one event. An error is handled like the error of an annotated listener method. */
  def onEvent(event: E): IO[Unit]

  /** Whether the listener runs on a fiber of its own instead of the fiber of the publisher, `false` by default. */
  def async: Boolean = false
}
