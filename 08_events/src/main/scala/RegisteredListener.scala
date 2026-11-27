package dev.coilix.events

import cats.effect.IO

/**
 * A listener found in a bean, with the way to call it.
 *
 * @param info
 *   the description of the listener
 * @param invoke
 *   calls the listener with one event and throws the exception of the listener itself
 */
private[events] final case class RegisteredListener(info: EventListenerInfo, invoke: AnyRef => IO[Unit])
