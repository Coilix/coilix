package dev.coilix.diagnostics

/**
 * Published by the [[dev.coilix.diagnostics.Health]] bean when the liveness or the readiness of the application changes.
 * Sources of work listen to it to stop taking new work while the application is `Degraded`.
 *
 * ```scala
 * @EventListener
 * def onHealth(event: HealthChanged): Unit =
 *   if (event.kind == HealthKind.Readiness) intake.pause(event.current != HealthStatus.Up)
 * ```
 *
 * @param reasons
 *   the reasons of the indicators that make the new status worse than `Up`, as `name: reason`
 */
final case class HealthChanged(kind: HealthKind, previous: HealthStatus, current: HealthStatus, reasons: List[String])
