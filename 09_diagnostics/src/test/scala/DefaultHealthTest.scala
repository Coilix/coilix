package dev.coilix.diagnostics

import munit.FunSuite

final class DefaultHealthTest extends FunSuite {

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
    memory.set(HealthState.degraded("memory is short"))
    health.refresh()
    assertEquals((health.liveness, health.readiness), (HealthStatus.Up, HealthStatus.Degraded))
    threads.set(HealthState.down("deadlock"))
    health.refresh()
    assertEquals((health.liveness, health.readiness), (HealthStatus.Down, HealthStatus.Down))
    assertEquals(health.indicators.map(_.name), List("memory", "threads"))
  }

  test("publishes HealthChanged only when a status changes, with the reasons of the indicators that are not Up") {
    val memory    = SettableHealthIndicator("memory", HealthKind.Readiness)
    val publisher = RecordingPublisher()
    val health    = DefaultHealth(List(memory), publisher)
    health.refresh()
    memory.set(HealthState.degraded("memory is short"))
    health.refresh()
    health.refresh()
    memory.set(HealthState.up)
    health.refresh()
    assertEquals(
      publisher.events,
      List(
        HealthChanged(HealthKind.Readiness, HealthStatus.Up, HealthStatus.Degraded, List("memory: memory is short")),
        HealthChanged(HealthKind.Readiness, HealthStatus.Degraded, HealthStatus.Up, Nil)
      )
    )
  }

  test("an indicator that throws is Down with the failure as the reason") {
    val broken = new HealthIndicator {
      override def name: String = "broken"

      override def kind: HealthKind = HealthKind.Liveness

      override def state: HealthState = throw IllegalStateException("no connection")
    }
    val health = DefaultHealth(List(broken), RecordingPublisher())
    health.refresh()
    assertEquals(health.liveness, HealthStatus.Down)
    assert(health.indicators.head.state.reason.contains("no connection"))
  }
}
