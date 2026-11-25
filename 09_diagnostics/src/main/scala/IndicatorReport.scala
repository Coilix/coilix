package dev.coilix.diagnostics

/** The state of one [[dev.coilix.diagnostics.HealthIndicator]] at the last check. */
final case class IndicatorReport(name: String, kind: HealthKind, state: HealthState)
