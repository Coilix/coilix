package dev.coilix.events
package listeners

import cats.effect.IO
import dev.coilix.annotation.Component
import dev.coilix.annotation.EventListener
import scala.concurrent.duration.DurationInt

@Component
final class ClosingWatcher {
  val sawBean: Recorder[Boolean] = Recorder()

  @EventListener(async = true)
  private def onClosing(event: ContextClosing): IO[Unit] =
    IO.sleep(100.millis) *> IO(sawBean.record(event.context.getBeanByName("closingWatcher").nonEmpty))
}
