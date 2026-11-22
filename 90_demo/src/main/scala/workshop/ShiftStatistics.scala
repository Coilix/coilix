package dev.coilix.demo
package workshop

import dev.coilix.annotation.Component
import dev.coilix.annotation.Slf4s
import dev.coilix.events.ApplicationEventListener
import java.util.concurrent.atomic.AtomicInteger

/**
 * Counts the finished inspections of the shift.
 *
 * It is an [[dev.coilix.events.ApplicationEventListener]], which runs synchronously, so the count is up to date as soon as
 * [[dev.coilix.demo.workshop.WorkshopShift]] has published the event.
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
}
