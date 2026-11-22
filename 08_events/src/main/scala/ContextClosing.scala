package dev.coilix.events

import dev.coilix.context.ApplicationContext

/**
 * Published when the context starts closing, in the [[dev.coilix.context.ClosingPhase.Announcement]] phase, before the sources of
 * new work stop and before any bean is destroyed.
 *
 * Closing waits for its asynchronous listeners like for any other asynchronous delivery. An exception of a listener is logged
 * and does not stop the closing.
 *
 * @param context
 *   the closing context, whose beans still work
 */
final case class ContextClosing(context: ApplicationContext)
