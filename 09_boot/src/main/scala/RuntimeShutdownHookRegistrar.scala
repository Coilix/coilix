package dev.coilix.boot

import dev.coilix.context.ConfigurableApplicationContext

final class RuntimeShutdownHookRegistrar(runtime: Runtime) extends ShutdownHookRegistrar {

  private val hookName = "coilix-shutdown-hook"

  override def register(context: ConfigurableApplicationContext): Thread = {
    val hook = Thread(() => context.close(), hookName)
    runtime.addShutdownHook(hook)
    hook
  }
}
