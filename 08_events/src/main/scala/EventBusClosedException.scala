package dev.coilix.events

import dev.coilix.common.CoilixException

/**
 * Thrown by [[dev.coilix.events.EventPublisher.publish]] when the context is closing and the caller is not a listener, or when
 * the context is closed, so no code counts on the delivery of an event that would be lost.
 */
final class EventBusClosedException(message: String) extends CoilixException(message)
