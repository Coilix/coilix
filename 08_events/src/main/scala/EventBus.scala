package dev.coilix.events

import scala.concurrent.duration.FiniteDuration

/**
 * The [[dev.coilix.events.EventPublisher]] of the events module, which also finds the listeners and delivers the events.
 *
 * The bus finds the listeners when every bean of the context exists. When the context closes, in the
 * [[dev.coilix.context.ClosingPhase.Events]] phase, it stops accepting events from outside listeners and waits for the running
 * asynchronous deliveries up to `coilix.events.shutdown-timeout`.
 */
trait EventBus extends EventPublisher {

  /** The listeners found in the context, sorted by name, or an empty list before the context has started. */
  def listeners: List[EventListenerInfo]

  /** The number of asynchronous deliveries that are running or waiting for a retry. */
  def pendingDeliveries: Int

  /**
   * Waits until no asynchronous delivery is running or waiting for a retry.
   *
   * @return
   *   `true` when the bus became idle, `false` when `timeout` passed first
   */
  def awaitIdle(timeout: FiniteDuration): Boolean
}
