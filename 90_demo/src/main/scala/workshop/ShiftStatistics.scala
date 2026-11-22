package dev.coilix.demo
package workshop

import dev.coilix.annotation.Component
import dev.coilix.annotation.EventListener
import dev.coilix.annotation.Slf4s
import dev.coilix.events.ApplicationEventListener
import dev.coilix.events.ApplicationStarted
import dev.coilix.events.ContextClosing
import java.util.concurrent.atomic.AtomicInteger

/**
 * Counts the finished inspections of the shift.
 *
 * It is an [[dev.coilix.events.ApplicationEventListener]], which runs synchronously, so the count is up to date as soon as
 * [[dev.coilix.demo.workshop.WorkshopShift]] has published the event. It also listens for the events of the framework: it
 * reports the count when the application has started, after the runners, and sums up the shift when the application closes.
 */
@Component
@Slf4s
final class ShiftStatistics extends ApplicationEventListener[InspectionFinished] {
  private val inspections = AtomicInteger()

  /** The number of inspections finished so far. */
  def inspectionsDone: Int = inspections.get

  override def onEvent(event: InspectionFinished): Unit = {
    val done = inspections.incrementAndGet()
    log.info(s"Shift statistics: $done inspection(s) done, the last one had ${event.steps} steps")
  }

  @EventListener
  private def onApplicationStarted(event: ApplicationStarted): Unit =
    log.info(s"The shift is under way with $inspectionsDone inspection(s) done at the start")

  @EventListener
  private def onContextClosing(event: ContextClosing): Unit =
    log.info(s"The shift ends with $inspectionsDone inspection(s) done")
}
