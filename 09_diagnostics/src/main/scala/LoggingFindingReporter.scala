package dev.coilix.diagnostics

import cats.effect.IO
import cats.effect.Ref
import cats.syntax.all.*
import dev.coilix.annotation.Slf4s
import java.time.Clock
import java.time.Duration
import java.time.Instant
import scala.concurrent.duration.FiniteDuration

/**
 * The [[dev.coilix.diagnostics.FindingReporter]] registered by the diagnostics module, which logs findings with the stacks of
 * their threads.
 *
 * A `Critical` finding is logged at `ERROR` level every round. A `Warning` (at `WARN`) or `Info` (at `INFO`) finding of a kind
 * that was logged less than `repeatAfter` ago is skipped, so a lasting problem does not flood the log.
 *
 * @param repeatAfter
 *   how long a kind of finding stays quiet after it was logged
 * @param maxThreads
 *   how many threads of one finding are logged
 * @param lastLogged
 *   when each kind of `Warning` or `Info` finding was logged last
 */
@Slf4s
case class LoggingFindingReporter(
  repeatAfter: FiniteDuration,
  maxThreads:  Int,
  clock:       Clock,
  lastLogged:  Ref[IO, Map[String, Instant]]
) extends FindingReporter {

  override def report(findings: List[Finding]): IO[Unit] =
    due(findings).flatMap(_.traverse_(logged))

  /** The findings of one round that are logged now; a `Warning` or `Info` kind logged within `repeatAfter` is left out. */
  private[diagnostics] def due(findings: List[Finding]): IO[List[Finding]] =
    IO(clock.instant).flatMap(now => lastLogged.modify(logged => selected(findings, now, logged)))

  /** The logged text of `finding`, with its threads and their stacks. */
  private[diagnostics] def textOf(finding: Finding): String = s"${finding.kind}: ${finding.message}${threadsOf(finding)}"

  private def selected(
    findings: List[Finding],
    now:      Instant,
    logged:   Map[String, Instant]
  ): (Map[String, Instant], List[Finding]) =
    findings.foldLeft((logged, List.empty[Finding]))(withFinding(now))

  private def withFinding(now: Instant)(state: (Map[String, Instant], List[Finding]), finding: Finding)
    : (Map[String, Instant], List[Finding]) = {
    val (seen, due) = state
    if (finding.severity == FindingSeverity.Critical) (seen, due :+ finding)
    else if (quietLongEnough(seen.get(finding.kind), now)) (seen.updated(finding.kind, now), due :+ finding)
    else (seen, due)
  }

  private def quietLongEnough(last: Option[Instant], now: Instant): Boolean =
    last.forall(previous => Duration.between(previous, now).toMillis >= repeatAfter.toMillis)

  private def logged(finding: Finding): IO[Unit] = {
    val text = textOf(finding)
    finding.severity match {
      case FindingSeverity.Critical => logF.error(text)
      case FindingSeverity.Warning  => logF.warn(text)
      case FindingSeverity.Info     => logF.info(text)
    }
  }

  private def threadsOf(finding: Finding): String =
    finding.threads.take(maxThreads).map(describe).mkString

  private def describe(thread: ThreadSnapshot): String = {
    val lock  = thread.lock.fold("")(name => s" waiting for $name")
    val owner = thread.lockOwner.fold("")(name => s" held by $name")
    val stack = thread.stack.map(frame => s"\n        at $frame").mkString
    s"\n    \"${thread.name}\" ${thread.state}$lock$owner$stack"
  }
}

/** Creation of the [[dev.coilix.diagnostics.LoggingFindingReporter]]. */
object LoggingFindingReporter {

  /** A reporter that has logged nothing yet. Creating the `Ref` runs no effect, so the reporter is a plain bean. */
  def apply(repeatAfter: FiniteDuration, maxThreads: Int, clock: Clock): LoggingFindingReporter =
    LoggingFindingReporter(repeatAfter, maxThreads, clock, Ref.unsafe[IO, Map[String, Instant]](Map.empty))
}
