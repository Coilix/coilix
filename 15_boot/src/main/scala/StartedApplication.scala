package dev.coilix.boot

import dev.coilix.context.ConfigurableApplicationContext

/**
 * An application whose context is built, returned by [[dev.coilix.boot.CoilixApplicationRunner.start]] before any runner has run.
 *
 * @param context
 *   the built context, closed by the release of the resource that gave it
 * @param arguments
 *   the parsed command-line arguments, to pass to the runners
 */
case class StartedApplication(context: ConfigurableApplicationContext, arguments: ApplicationArguments)
