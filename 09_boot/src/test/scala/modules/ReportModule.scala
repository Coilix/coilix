package dev.coilix.boot
package modules

import dev.coilix.context.CoilixModule

final class ReportModule extends CoilixModule {
  override def name: String = "report"

  override def components: List[Class[?]] = List(classOf[ReportConfiguration])
}
