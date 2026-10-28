package org.scaling.boot

import org.scaling.config.ComponentProvider
import org.scaling.config.ConfigPath
import org.scaling.context.ConfigurableApplicationContext

trait ShutdownHookRegistrar {
  def register(context: ConfigurableApplicationContext): Thread
}

object ShutdownHookRegistrar {
  given ConfigPath[ShutdownHookRegistrar] = ConfigPath[ShutdownHookRegistrar]("scaling.boot.shutdown-hook-registrar")

  def default(using components: ComponentProvider): ShutdownHookRegistrar =
    components.get[ShutdownHookRegistrar](RuntimeShutdownHookRegistrar(Runtime.getRuntime))
}
