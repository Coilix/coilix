package dev.coilix.diagnostics

import cats.effect.IO
import cats.syntax.all.*
import dev.coilix.annotation.Slf4s
import dev.coilix.events.EventPublisher
import java.util.concurrent.atomic.AtomicReference
import scala.util.control.NonFatal

/**
 * The [[dev.coilix.diagnostics.Health]] registered by the diagnostics module as the `health` bean. An indicator that throws
 * counts as `Down` with the exception as the reason. Before the first refresh both statuses are `Up`.
 *
 * @param sources
 *   every indicator of the context
 * @param publisher
 *   publishes [[dev.coilix.diagnostics.HealthChanged]]
 */
@Slf4s
final class DefaultHealth(sources: List[HealthIndicator], publisher: EventPublisher) extends Health {

  private val current = AtomicReference(List.empty[IndicatorReport])

  override def liveness: HealthStatus = livenessOf(current.get)

  override def readiness: HealthStatus = readinessOf(current.get)

  override def indicators: List[IndicatorReport] = current.get

  override def refresh(): IO[Unit] =
    IO(changes()).flatMap(_.traverse_(announced))

  private def changes(): List[HealthChanged] = {
    val next     = sources.map(reportOf).sortBy(_.name)
    val previous = current.getAndSet(next)
    List(
      changed(HealthKind.Liveness, livenessOf(previous), livenessOf(next), next.filter(_.kind == HealthKind.Liveness)),
      changed(HealthKind.Readiness, readinessOf(previous), readinessOf(next), next)
    ).flatten
  }

  private def reportOf(indicator: HealthIndicator): IndicatorReport =
    IndicatorReport(indicator.name, indicator.kind, stateOf(indicator))

  private def stateOf(indicator: HealthIndicator): HealthState =
    try {
      indicator.state
    } catch {
      case NonFatal(failure) => HealthState.down(s"the indicator failed: $failure")
    }

  private def livenessOf(reports: List[IndicatorReport]): HealthStatus =
    HealthStatus.worst(reports.filter(_.kind == HealthKind.Liveness).map(_.state.status))

  private def readinessOf(reports: List[IndicatorReport]): HealthStatus = HealthStatus.worst(reports.map(_.state.status))

  private def changed(
    kind:     HealthKind,
    previous: HealthStatus,
    next:     HealthStatus,
    counted:  List[IndicatorReport]
  ): Option[HealthChanged] =
    Option.when(previous != next)(
      HealthChanged(
        kind,
        previous,
        next,
        counted.filter(_.state.status != HealthStatus.Up).map(report => s"${report.name}: ${report.state.reason}")
      )
    )

  private def announced(change: HealthChanged): IO[Unit] =
    logF.info(s"${change.kind} changed from ${change.previous} to ${change.current}${describe(change.reasons)}") *>
      publisher.publish(change)

  private def describe(reasons: List[String]): String =
    if (reasons.isEmpty) "" else reasons.mkString(" (", ", ", ")")
}
