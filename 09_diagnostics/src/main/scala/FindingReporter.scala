package dev.coilix.diagnostics

import cats.effect.IO

/**
 * Tells about the [[dev.coilix.diagnostics.Finding]]s of one round of checks. The diagnostics module registers
 * [[dev.coilix.diagnostics.LoggingFindingReporter]], and a bean of this type in the application replaces it, for example to
 * send findings to an alerting system.
 */
trait FindingReporter {

  /** Reports the findings of one round, which may be empty. */
  def report(findings: List[Finding]): IO[Unit]
}
