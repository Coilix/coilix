package dev.coilix.boot

import dev.coilix.config.ComponentProvider
import dev.coilix.config.ConfigPath
import dev.coilix.context.ConfigurableApplicationContext

trait ShutdownHookRegistrar {
  def register(context: ConfigurableApplicationContext): Thread
}

object ShutdownHookRegistrar {
  given ConfigPath[ShutdownHookRegistrar] = ConfigPath[ShutdownHookRegistrar]("coilix.boot.shutdown-hook-registrar")

  def default(using components: ComponentProvider): ShutdownHookRegistrar =
    components.get[ShutdownHookRegistrar](RuntimeShutdownHookRegistrar(Runtime.getRuntime))
}
