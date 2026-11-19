package dev.coilix.context

import dev.coilix.config.ComponentProvider
import dev.coilix.context.collected.CleanupTask
import dev.coilix.context.collected.ReportTask
import dev.coilix.context.lifecycle.Journal

object CollectedContexts {
  def withTasks(hocon: String = ""): ConfigurableApplicationContext =
    contextOf(hocon, List(classOf[Journal]), List(classOf[CleanupTask], classOf[ReportTask]))

  def contextOf(hocon: String, beans: List[Class[?]], collected: List[Class[?]]): ConfigurableApplicationContext = {
    given ComponentProvider = Providers.fromHocon(hocon)
    ApplicationContextBuilder.default.build(Definitions.of(beans*) :+ ComponentCollector.default.collect(collected))
  }
}
