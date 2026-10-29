package io.github.coilix.boot

import io.github.coilix.config.ComponentProvider
import io.github.coilix.config.ConfigPath
import io.github.coilix.context.ApplicationContext

trait RunnerInvoker {
  def invoke(context: ApplicationContext, arguments: ApplicationArguments): Unit
}

object RunnerInvoker {
  given ConfigPath[RunnerInvoker] = ConfigPath[RunnerInvoker]("coilix.boot.runner-invoker")

  def default(using components: ComponentProvider): RunnerInvoker =
    components.get[RunnerInvoker](OrderedRunnerInvoker())
}
