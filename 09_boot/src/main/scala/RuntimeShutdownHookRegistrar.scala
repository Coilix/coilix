package org.scaling.boot

import org.scaling.context.ConfigurableApplicationContext

final class RuntimeShutdownHookRegistrar(runtime: Runtime) extends ShutdownHookRegistrar {

  private val hookName = "scaling-shutdown-hook"

  override def register(context: ConfigurableApplicationContext): Thread = {
    val hook = Thread(() => context.close(), hookName)
    runtime.addShutdownHook(hook)
    hook
  }
}
