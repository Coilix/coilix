package dev.coilix.diagnostics

import dev.coilix.annotation.EventListener
import dev.coilix.events.ApplicationFailed
import dev.coilix.events.ApplicationStarted

/**
 * Moves the [[dev.coilix.diagnostics.StartupHealthIndicator]] when the start ends and refreshes the
 * [[dev.coilix.diagnostics.Health]] at once, so readiness changes without waiting for the next round of diagnostics.
 */
case class StartupHealthListener(indicator: StartupHealthIndicator, health: Health) {

  @EventListener
  private def onStarted(event: ApplicationStarted): Unit = {
    indicator.started()
    health.refresh()
  }

  @EventListener
  private def onFailed(event: ApplicationFailed): Unit = {
    indicator.failed(String.valueOf(event.failure.getMessage))
    health.refresh()
  }
}
