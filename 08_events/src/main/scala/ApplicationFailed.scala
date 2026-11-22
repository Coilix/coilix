package dev.coilix.events

import dev.coilix.context.ApplicationContext

/**
 * Published when the startup fails after the context was built: a listener of [[dev.coilix.events.ContextReady]] or
 * [[dev.coilix.events.ApplicationStarted]], or an `ApplicationRunner`, has thrown an exception. The context closes right after
 * the event.
 *
 * A failure while the beans are created is not published, because no listener exists yet. An exception of a listener is logged
 * and does not stop the closing.
 *
 * @param context
 *   the context that is about to close
 * @param failure
 *   the exception that stopped the startup
 */
final case class ApplicationFailed(context: ApplicationContext, failure: Throwable)
