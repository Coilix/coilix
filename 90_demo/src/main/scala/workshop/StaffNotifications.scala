package dev.coilix.demo
package workshop

import dev.coilix.annotation.Component
import dev.coilix.annotation.EventListener

/**
 * Tells the staff about finished inspections through the [[dev.coilix.demo.workshop.Notifier]] of the active profile.
 *
 * The listener is asynchronous, so a slow notification does not hold up the shift. When sending fails, it is tried again with
 * the policy under `coilix.events.listeners.staffNotifications.onInspectionFinished.retry` in `application.yaml`.
 */
@Component
final class StaffNotifications(notifier: Notifier) {

  @EventListener(async = true)
  private def onInspectionFinished(event: InspectionFinished): Unit =
    notifier.send(s"${event.mechanic} finished the inspection of ${event.vin.value}")
}
