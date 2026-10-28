package org.scaling.boot

import org.scaling.config.ComponentProvider

final case class StartupComponents(
  resolver:      BasePackageResolver,
  bootstrap:     ScalingBootstrap,
  invoker:       RunnerInvoker,
  startupLogger: StartupLogger,
  shutdownHooks: ShutdownHookRegistrar
)

object StartupComponents {
  def default(using components: ComponentProvider): StartupComponents =
    StartupComponents(
      BasePackageResolver.default,
      ScalingBootstrap.default,
      RunnerInvoker.default,
      StartupLogger.default,
      ShutdownHookRegistrar.default
    )
}
