package dev.coilix.boot

import dev.coilix.config.ComponentProvider

/**
 * The replaceable components used during startup, created from the loaded configuration.
 *
 * @param resolver
 *   decides which packages to scan
 * @param bootstrap
 *   builds the context
 * @param invoker
 *   calls the runners
 * @param startupLogger
 *   logs the start and the startup time
 * @param shutdownHooks
 *   closes the context when the JVM stops
 */
final case class StartupComponents(
  resolver:      BasePackageResolver,
  bootstrap:     CoilixBootstrap,
  invoker:       RunnerInvoker,
  startupLogger: StartupLogger,
  shutdownHooks: ShutdownHookRegistrar
)

/** Factories for [[dev.coilix.boot.StartupComponents]]. */
object StartupComponents {

  /** Each component as configured, or its default. */
  def default(using components: ComponentProvider): StartupComponents =
    StartupComponents(
      BasePackageResolver.default,
      CoilixBootstrap.default,
      RunnerInvoker.default,
      StartupLogger.default,
      ShutdownHookRegistrar.default
    )
}
