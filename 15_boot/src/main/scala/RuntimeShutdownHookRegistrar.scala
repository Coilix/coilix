package dev.coilix.boot

import dev.coilix.concurrency.FatalErrorHandler
import dev.coilix.concurrency.FatalErrors
import dev.coilix.context.ConfigurableApplicationContext

/**
 * Registers a JVM shutdown hook named `coilix-shutdown-hook` that closes the context, and makes the
 * [[dev.coilix.concurrency.FatalErrorHandler]] of the context the default uncaught exception handler of the process, so a fatal
 * error in any thread ends the application.
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
    context.getBeanOption[FatalErrorHandler].foreach(handler =>
      Thread.setDefaultUncaughtExceptionHandler(FatalErrors.uncaughtExceptionHandler(handler))
    )
    hook
  }
}
