package dev.coilix.events

import dev.coilix.common.GenericType

/** An event published before the context has started, kept until the listeners are known. */
private[events] final case class EarlyEvent(event: AnyRef, eventType: GenericType)
