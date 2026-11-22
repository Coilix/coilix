package dev.coilix.events

import dev.coilix.context.ApplicationContext

/**
 * Published when every bean exists and every [[dev.coilix.context.ContextLifecycleListener]] has started, before the first
 * `ApplicationRunner` runs.
 *
 * An exception of a synchronous listener stops the startup like an exception of a runner.
 *
 * ```scala
 * @Component
 * final class CacheWarmer(catalog: Catalog) {
 *   @EventListener(async = true)
 *   private def onContextReady(event: ContextReady): Unit = catalog.preload()
 * }
 * ```
 *
 * @param context
 *   the started context
 */
final case class ContextReady(context: ApplicationContext)
