package dev.coilix.boot

import dev.coilix.config.ComponentProvider
import dev.coilix.config.ConfigPath
import dev.coilix.context.ApplicationContext

/** Calls the [[dev.coilix.boot.ApplicationRunner]] beans once the context is ready. */
trait RunnerInvoker {

  /** Calls the runners found in `context` with `arguments`. */
  def invoke(context: ApplicationContext, arguments: ApplicationArguments): Unit
}

/** Configuration path and default implementation of [[dev.coilix.boot.RunnerInvoker]]. */
object RunnerInvoker {

  /**
   * Replace the component in the configuration under `coilix.boot.runner-invoker`, with a `class` key naming the implementation.
   */
  given ConfigPath[RunnerInvoker] = ConfigPath[RunnerInvoker]("coilix.boot.runner-invoker")

  /** The configured implementation, or [[dev.coilix.boot.OrderedRunnerInvoker]] when `coilix.boot.runner-invoker` is not set. */
  def default(using components: ComponentProvider): RunnerInvoker =
    components.get[RunnerInvoker](OrderedRunnerInvoker())
}
