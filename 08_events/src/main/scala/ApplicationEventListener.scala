package dev.coilix.events

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
 *   override def onEvent(event: OrderPlaced): Unit = { val _ = placed.incrementAndGet() }
 * }
 * ```
 *
 * @tparam E
 *   the type of the events
 */
trait ApplicationEventListener[E] {

  /** Handles one event. An exception is handled like the exception of an annotated listener method. */
  def onEvent(event: E): Unit

  /** Whether the listener runs on the task executor instead of the thread of the publisher, `false` by default. */
  def async: Boolean = false

  /** Whether an asynchronous listener blocks and runs on the blocking executor, `false` by default. */
  def blocking: Boolean = false
}
