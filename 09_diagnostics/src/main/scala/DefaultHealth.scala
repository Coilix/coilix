package dev.coilix.diagnostics

import cats.effect.IO
import cats.effect.Ref
import cats.syntax.all.*
import dev.coilix.annotation.Slf4s
import dev.coilix.events.EventPublisher

/**
 * The [[dev.coilix.diagnostics.Health]] registered by the diagnostics module as the `health` bean. An indicator that throws
 * counts as `Down` with the exception as the reason. Before the first refresh both statuses are `Up`.
 *
 * @param sources
 *   every indicator of the context
 * @param publisher
 *   publishes [[dev.coilix.diagnostics.HealthChanged]]
 * @param current
 *   the reports of the last refresh
 */
@Slf4s
case class DefaultHealth(sources: List[HealthIndicator], publisher: EventPublisher, current: Ref[IO, List[IndicatorReport]])
    extends Health {

  override def liveness: IO[HealthStatus] = current.get.map(livenessOf)

  override def readiness: IO[HealthStatus] = current.get.map(readinessOf)

  override def indicators: IO[List[IndicatorReport]] = current.get

  override def refresh(): IO[Unit] =
    sources
      .traverse(reportOf)
      .map(_.sortBy(_.name))
      .flatMap(next => current.getAndSet(next).map(previous => changes(previous, next)))
      .flatMap(_.traverse_(announced))

  private def changes(previous: List[IndicatorReport], next: List[IndicatorReport]): List[HealthChanged] =
    List(
      changed(HealthKind.Liveness, livenessOf(previous), livenessOf(next), next.filter(_.kind == HealthKind.Liveness)),
      changed(HealthKind.Readiness, readinessOf(previous), readinessOf(next), next)
    ).flatten

  private def reportOf(indicator: HealthIndicator): IO[IndicatorReport] =
    stateOf(indicator).map(state => IndicatorReport(indicator.name, indicator.kind, state))

  private def stateOf(indicator: HealthIndicator): IO[HealthState] =
    IO.defer(indicator.state).handleError(failure => HealthState.down(s"the indicator failed: $failure"))

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

/** Creation of the [[dev.coilix.diagnostics.DefaultHealth]]. */
object DefaultHealth {

  /** A health without reports, so both statuses are `Up` until the first refresh. Creating the `Ref` runs no effect. */
  def apply(sources: List[HealthIndicator], publisher: EventPublisher): DefaultHealth =
    DefaultHealth(sources, publisher, Ref.unsafe[IO, List[IndicatorReport]](Nil))
}
