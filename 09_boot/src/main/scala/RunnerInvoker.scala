package org.scaling.boot

import org.scaling.config.ComponentProvider
import org.scaling.config.ConfigPath
import org.scaling.context.ApplicationContext

trait RunnerInvoker {
  def invoke(context: ApplicationContext, arguments: ApplicationArguments): Unit
}

object RunnerInvoker {
  given ConfigPath[RunnerInvoker] = ConfigPath[RunnerInvoker]("scaling.boot.runner-invoker")

  def default(using components: ComponentProvider): RunnerInvoker =
    components.get[RunnerInvoker](OrderedRunnerInvoker())
}
