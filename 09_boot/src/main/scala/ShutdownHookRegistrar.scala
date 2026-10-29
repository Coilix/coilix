package io.github.coilix.boot

import io.github.coilix.config.ComponentProvider
import io.github.coilix.config.ConfigPath
import io.github.coilix.context.ConfigurableApplicationContext

trait ShutdownHookRegistrar {
  def register(context: ConfigurableApplicationContext): Thread
}

object ShutdownHookRegistrar {
  given ConfigPath[ShutdownHookRegistrar] = ConfigPath[ShutdownHookRegistrar]("coilix.boot.shutdown-hook-registrar")

  def default(using components: ComponentProvider): ShutdownHookRegistrar =
    components.get[ShutdownHookRegistrar](RuntimeShutdownHookRegistrar(Runtime.getRuntime))
}
