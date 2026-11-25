package dev.coilix.diagnostics

import dev.coilix.annotation.Slf4s
import java.time.Clock
import java.time.Duration
import java.time.Instant
import java.util.concurrent.atomic.AtomicReference
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
 */
@Slf4s
final class LoggingFindingReporter(repeatAfter: FiniteDuration, maxThreads: Int, clock: Clock) extends FindingReporter {

  private val lastLogged = AtomicReference(Map.empty[String, Instant])

  override def report(findings: List[Finding]): Unit =
    due(findings).foreach(logged)

  /** The findings of one round that are logged now; a `Warning` or `Info` kind logged within `repeatAfter` is left out. */
  private[diagnostics] def due(findings: List[Finding]): List[Finding] = {
    val now = clock.instant
    findings.filter(dueAt(now))
  }

  /** The logged text of `finding`, with its threads and their stacks. */
  private[diagnostics] def textOf(finding: Finding): String = s"${finding.kind}: ${finding.message}${threadsOf(finding)}"

  private def dueAt(now: Instant)(finding: Finding): Boolean =
    finding.severity == FindingSeverity.Critical || quietLongEnough(finding.kind, now)

  private def quietLongEnough(kind: String, now: Instant): Boolean = {
    val previous = lastLogged.get
    val due      = previous.get(kind).forall(last => Duration.between(last, now).toMillis >= repeatAfter.toMillis)
    due && lastLogged.compareAndSet(previous, previous.updated(kind, now))
  }

  private def logged(finding: Finding): Unit = {
    val text = textOf(finding)
    finding.severity match {
      case FindingSeverity.Critical => log.error(text)
      case FindingSeverity.Warning  => log.warn(text)
      case FindingSeverity.Info     => log.info(text)
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
