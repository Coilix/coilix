package dev.coilix.events

import dev.coilix.context.ApplicationContext

/**
 * Published after every `ApplicationRunner` has finished, when the application is fully started.
 *
 * A test context publishes it right after [[dev.coilix.events.ContextReady]] when its runners do not run. An exception of a
 * synchronous listener stops the startup like an exception of a runner.
 *
 * @param context
 *   the started context
 */
final case class ApplicationStarted(context: ApplicationContext)
