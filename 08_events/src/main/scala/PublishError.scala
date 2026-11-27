package dev.coilix.events

import dev.coilix.common.GenericType

/**
 * A predictable reason why an event cannot be published, returned by [[dev.coilix.events.EventPublisher.tryPublish]].
 *
 * A failing listener is not a `PublishError`: its exception is thrown to the publisher.
 */
enum PublishError {

  /**
   * The context is closing and the publisher is not a listener.
   *
   * @param eventType
   *   the type of the refused event
   */
  case Closing(eventType: GenericType)

  /**
   * The context is closed.
   *
   * @param eventType
   *   the type of the refused event
   */
  case Closed(eventType: GenericType)

  /** The description of this error. */
  def message: String =
    this match {
      case Closing(eventType) => s"Cannot publish an event of type $eventType, the context is closing"
      case Closed(eventType)  => s"Cannot publish an event of type $eventType, the context is closed"
    }

  /** The exception that reports this error to code that throws. */
  def exception: EventBusClosedException = EventBusClosedException(message)
}
