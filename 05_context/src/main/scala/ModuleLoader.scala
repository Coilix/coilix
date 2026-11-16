package dev.coilix.context

import dev.coilix.config.ComponentProvider
import dev.coilix.config.ConfigPath

/** Finds the enabled [[dev.coilix.context.CoilixModule]]s of the application. */
trait ModuleLoader {

  /** The enabled modules, sorted by name. */
  def load(): List[CoilixModule]
}

/** Configuration path and default implementation of [[dev.coilix.context.ModuleLoader]]. */
object ModuleLoader {

  /** Replace the component in the configuration under `coilix.context.module-loader`, with a `class` key naming the implementation. */
  given ConfigPath[ModuleLoader] = ConfigPath[ModuleLoader]("coilix.context.module-loader")

  /**
   * The configured implementation, or a [[dev.coilix.context.ServiceLoaderModuleLoader]] that uses the context class loader of
   * the current thread when `coilix.context.module-loader` is not set.
   */
  def default(using components: ComponentProvider): ModuleLoader =
    components.get[ModuleLoader](
      ServiceLoaderModuleLoader(Thread.currentThread().getContextClassLoader, components.environment)
    )
}
