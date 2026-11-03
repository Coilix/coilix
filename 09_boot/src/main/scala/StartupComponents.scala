package dev.coilix.boot

import dev.coilix.config.ComponentProvider

final case class StartupComponents(
  resolver:      BasePackageResolver,
  bootstrap:     CoilixBootstrap,
  invoker:       RunnerInvoker,
  startupLogger: StartupLogger,
  shutdownHooks: ShutdownHookRegistrar
)

object StartupComponents {
  def default(using components: ComponentProvider): StartupComponents =
    StartupComponents(
      BasePackageResolver.default,
      CoilixBootstrap.default,
      RunnerInvoker.default,
      StartupLogger.default,
      ShutdownHookRegistrar.default
    )
}
