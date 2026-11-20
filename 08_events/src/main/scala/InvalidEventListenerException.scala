package dev.coilix.events

import dev.coilix.common.CoilixException

/**
 * Thrown at startup when a listener cannot be registered, for example because an [[dev.coilix.annotation.EventListener]] method
 * takes two parameters or the event type of an [[dev.coilix.events.ApplicationEventListener]] is unknown. The message names the
 * bean and the method.
 */
final class InvalidEventListenerException(message: String) extends CoilixException(message)
