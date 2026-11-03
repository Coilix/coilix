package dev.coilix.boot

import dev.coilix.config.ComponentProvider
import dev.coilix.config.ConfigPath
import dev.coilix.context.ApplicationContext

trait RunnerInvoker {
  def invoke(context: ApplicationContext, arguments: ApplicationArguments): Unit
}

object RunnerInvoker {
  given ConfigPath[RunnerInvoker] = ConfigPath[RunnerInvoker]("coilix.boot.runner-invoker")

  def default(using components: ComponentProvider): RunnerInvoker =
    components.get[RunnerInvoker](OrderedRunnerInvoker())
}
