package dev.coilix.diagnostics

/** How serious a [[dev.coilix.diagnostics.Finding]] is. */
enum FindingSeverity {

  /** Worth knowing, logged at `INFO` level. */
  case Info

  /** Probably a problem, logged at `WARN` level. */
  case Warning

  /** A problem the application does not recover from by itself, logged at `ERROR` level every time. */
  case Critical
}
