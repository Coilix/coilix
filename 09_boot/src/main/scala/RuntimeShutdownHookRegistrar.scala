package dev.coilix.boot

import dev.coilix.context.ConfigurableApplicationContext

/**
 * Registers a JVM shutdown hook named `coilix-shutdown-hook` that closes the context.
 *
 * This is the default [[dev.coilix.boot.ShutdownHookRegistrar]].
 *
 * @param runtime
 *   the runtime that receives the hook
 */
final class RuntimeShutdownHookRegistrar(runtime: Runtime) extends ShutdownHookRegistrar {

  private val hookName = "coilix-shutdown-hook"

  override def register(context: ConfigurableApplicationContext): Thread = {
    val hook = Thread(() => context.close(), hookName)
    runtime.addShutdownHook(hook)
    hook
  }
}
