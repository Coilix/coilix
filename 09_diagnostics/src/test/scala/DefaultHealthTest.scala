package dev.coilix.diagnostics

import cats.effect.IO
import cats.syntax.all.*
import munit.CatsEffectSuite

final class DefaultHealthTest extends CatsEffectSuite {

  test("the worst status wins and an empty list is Up") {
    assertEquals(
      (HealthStatus.worst(Nil), HealthStatus.worst(List(HealthStatus.Degraded, HealthStatus.Up, HealthStatus.Down))),
      (HealthStatus.Up, HealthStatus.Down)
    )
  }

  test("liveness counts liveness indicators and readiness counts every indicator") {
    val threads = SettableHealthIndicator("threads", HealthKind.Liveness)
    val memory  = SettableHealthIndicator("memory", HealthKind.Readiness)
    val health  = DefaultHealth(List(threads, memory), RecordingPublisher())
    for {
      _      <- memory.set(HealthState.degraded("memory is short")) *> health.refresh()
      first  <- statuses(health)
      _      <- threads.set(HealthState.down("deadlock")) *> health.refresh()
      second <- statuses(health)
      names  <- health.indicators.map(_.map(_.name))
    } yield assertEquals(
      (first, second, names),
      ((HealthStatus.Up, HealthStatus.Degraded), (HealthStatus.Down, HealthStatus.Down), List("memory", "threads"))
    )
  }

  test("publishes HealthChanged only when a status changes, with the reasons of the indicators that are not Up") {
    val memory    = SettableHealthIndicator("memory", HealthKind.Readiness)
    val publisher = RecordingPublisher()
    val health    = DefaultHealth(List(memory), publisher)
    for {
      _ <- health.refresh()
      _ <- memory.set(HealthState.degraded("memory is short")) *> health.refresh() *> health.refresh()
      _ <- memory.set(HealthState.up) *> health.refresh()
    } yield assertEquals(
      publisher.events,
      List(
        HealthChanged(HealthKind.Readiness, HealthStatus.Up, HealthStatus.Degraded, List("memory: memory is short")),
        HealthChanged(HealthKind.Readiness, HealthStatus.Degraded, HealthStatus.Up, Nil)
      )
    )
  }

  test("an indicator that throws or fails is Down with the failure as the reason") {
    val thrown = broken("thrown", throw IllegalStateException("no connection"))
    val raised = broken("raised", IO.raiseError(IllegalStateException("no disk")))
    val health = DefaultHealth(List(thrown, raised), RecordingPublisher())
    for {
      _          <- health.refresh()
      liveness   <- health.liveness
      indicators <- health.indicators
    } yield assertEquals(
      (liveness, indicators.map(_.state.reason)),
      (
        HealthStatus.Down,
        List(
          "the indicator failed: java.lang.IllegalStateException: no disk",
          "the indicator failed: java.lang.IllegalStateException: no connection"
        )
      )
    )
  }

  private def statuses(health: Health): IO[(HealthStatus, HealthStatus)] = (health.liveness, health.readiness).tupled

  private def broken(indicatorName: String, failing: => IO[HealthState]): HealthIndicator =
    new HealthIndicator {
      override def name: String = indicatorName

      override def kind: HealthKind = HealthKind.Liveness

      override def state: IO[HealthState] = failing
    }
}
