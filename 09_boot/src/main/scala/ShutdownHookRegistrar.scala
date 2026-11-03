package dev.coilix.boot

import dev.coilix.config.ComponentProvider
import dev.coilix.config.ConfigPath
import dev.coilix.context.ConfigurableApplicationContext

/** Makes sure the application context is closed when the JVM stops. */
trait ShutdownHookRegistrar {

  /**
   * Registers a hook that closes `context`.
   *
   * @return
   *   the hook thread, which can be removed with `Runtime.removeShutdownHook`
   */
  def register(context: ConfigurableApplicationContext): Thread
}

/** Configuration path and default implementation of [[dev.coilix.boot.ShutdownHookRegistrar]]. */
object ShutdownHookRegistrar {

  /**
   * Replace the component in the configuration under `coilix.boot.shutdown-hook-registrar`, with a `class` key naming the
   * implementation.
   */
  given ConfigPath[ShutdownHookRegistrar] = ConfigPath[ShutdownHookRegistrar]("coilix.boot.shutdown-hook-registrar")

  /**
   * The configured implementation, or [[dev.coilix.boot.RuntimeShutdownHookRegistrar]] when `coilix.boot.shutdown-hook-registrar`
   * is not set.
   */
  def default(using components: ComponentProvider): ShutdownHookRegistrar =
    components.get[ShutdownHookRegistrar](RuntimeShutdownHookRegistrar(Runtime.getRuntime))
}
