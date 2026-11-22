package dev.coilix.events
package listeners

import dev.coilix.annotation.Component
import dev.coilix.annotation.EventListener

@Component
final class ClosingWatcher {
  val sawBean: Recorder[Boolean] = Recorder()

  @EventListener(async = true)
  private def onClosing(event: ContextClosing): Unit = {
    Thread.sleep(100)
    sawBean.record(event.context.getBeanByName("closingWatcher").nonEmpty)
  }
}
