package dev.coilix.demo
package workshop

import cats.effect.IO
import dev.coilix.annotation.Component
import dev.coilix.annotation.EventListener

/**
 * Tells the staff about finished inspections through the [[dev.coilix.demo.workshop.Notifier]] of the active profile.
 *
 * The listener is asynchronous and sends in `IO.blocking`, so a slow notification does not hold up the shift or a thread of
 * the compute pool. When sending fails, it is tried again with
 * the policy under `coilix.events.listeners.staffNotifications.onInspectionFinished.retry` in `application.yaml`.
 */
@Component
final class StaffNotifications(notifier: Notifier) {

  @EventListener(async = true)
  private def onInspectionFinished(event: InspectionFinished): IO[Unit] =
    IO.blocking(notifier.send(s"${event.mechanic} finished the inspection of ${event.vin.value}"))
}
