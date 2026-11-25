package dev.coilix.diagnostics

/** Which question a [[dev.coilix.diagnostics.HealthIndicator]] answers. */
enum HealthKind {

  /**
   * Whether the application is alive. A liveness indicator that is not `Up` means the application should be restarted, so it
   * counts for both liveness and readiness.
   */
  case Liveness

  /** Whether the application should get new work. It counts only for readiness. */
  case Readiness
}
