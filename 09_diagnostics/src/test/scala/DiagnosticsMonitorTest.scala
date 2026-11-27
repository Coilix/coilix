package dev.coilix.diagnostics

import cats.effect.unsafe.IORuntime
import java.time.Clock
import munit.FunSuite
import scala.concurrent.duration.DurationInt

final class DiagnosticsMonitorTest extends FunSuite {

  test("hands a deadlock to the fatal error handler once in the shutdown mode") {
    val fatalErrors = RecordingFatalErrorHandler()
    val monitor     = monitorOf(DeadlockAction.Shutdown, fatalErrors, SettableHealthIndicator("threads", HealthKind.Liveness))
    val deadlock    = LockDeadlock("shutdown-deadlock")
    try {
      deadlock.awaitDeadlock()
      val findings = monitor.runChecks()
      val _        = monitor.runChecks()
      assertEquals(findings.filter(_.kind == Finding.deadlock).map(_.severity), List(FindingSeverity.Critical))
      assertEquals(fatalErrors.failures.map(_.getClass), List(classOf[ThreadDeadlockException]))
      assert(fatalErrors.failures.head.getMessage.contains("shutdown-deadlock-1"), fatalErrors.failures.head.getMessage)
    } finally {
      deadlock.close()
    }
  }

  test("sets the threads indicator Down and fails liveness in the report mode") {
    val indicator   = SettableHealthIndicator("threads", HealthKind.Liveness)
    val health      = DefaultHealth(List(indicator), RecordingPublisher())
    val fatalErrors = RecordingFatalErrorHandler()
    val monitor     = monitorOf(DeadlockAction.Report, fatalErrors, indicator, health)
    val deadlock    = LockDeadlock("report-deadlock")
    try {
      deadlock.awaitDeadlock()
      val _ = monitor.runChecks()
      assertEquals((health.liveness, fatalErrors.failures), (HealthStatus.Down, Nil))
      assert(indicator.state.reason.contains("report-deadlock-2"), indicator.state.reason)
    } finally {
      deadlock.close()
    }
  }

  test("refreshes the health but runs no thread checks when they are disabled") {
    val memory  = SettableHealthIndicator("memory", HealthKind.Readiness)
    val health  = DefaultHealth(List(memory), RecordingPublisher())
    val monitor = DiagnosticsMonitor(
      DiagnosticsSettings(threads = ThreadMonitorSettings(enabled = false)),
      ThreadSampler(5),
      List(ThreadCountCheck(0, 0)),
      LoggingFindingReporter(1.minute, 10, Clock.systemUTC),
      SettableHealthIndicator("threads", HealthKind.Liveness),
      health,
      RecordingFatalErrorHandler(),
      Clock.systemUTC,
      IORuntime.global
    )
    memory.set(HealthState.degraded("memory is short"))
    assertEquals((monitor.runChecks(), health.readiness), (Nil, HealthStatus.Degraded))
  }

  test("a failing check is skipped and the other checks still run") {
    val failing = new ThreadCheck {
      override def check(sample: ThreadSample, history: ThreadHistory): List[Finding] = throw IllegalStateException("broken")
    }
    val monitor = DiagnosticsMonitor(
      DiagnosticsSettings(),
      ThreadSampler(5),
      List(failing, ThreadCountCheck(1, Int.MaxValue)),
      LoggingFindingReporter(1.minute, 10, Clock.systemUTC),
      SettableHealthIndicator("threads", HealthKind.Liveness),
      DefaultHealth(Nil, RecordingPublisher()),
      RecordingFatalErrorHandler(),
      Clock.systemUTC,
      IORuntime.global
    )
    assertEquals(monitor.runChecks().map(_.kind), List(Finding.tooManyThreads))
  }

  test("a round survives a LinkageError of a check and hands only a VirtualMachineError to the fatal error handler") {
    val fatalErrors = RecordingFatalErrorHandler()
    def throwing(failure: Throwable): DiagnosticsMonitor =
      DiagnosticsMonitor(
        DiagnosticsSettings(),
        ThreadSampler(5),
        List((_, _) => throw failure),
        LoggingFindingReporter(1.minute, 10, Clock.systemUTC),
        SettableHealthIndicator("threads", HealthKind.Liveness),
        DefaultHealth(Nil, RecordingPublisher()),
        fatalErrors,
        Clock.systemUTC,
        IORuntime.global
      )
    throwing(NoClassDefFoundError("dev/coilix/diagnostics/Missing")).round()
    throwing(OutOfMemoryError("test")).round()
    assertEquals(fatalErrors.failures.map(_.getClass), List(classOf[OutOfMemoryError]))
  }

  private def monitorOf(
    action:      DeadlockAction,
    fatalErrors: RecordingFatalErrorHandler,
    indicator:   SettableHealthIndicator,
    health:      Health = DefaultHealth(Nil, RecordingPublisher())
  ): DiagnosticsMonitor =
    DiagnosticsMonitor(
      DiagnosticsSettings(threads = ThreadMonitorSettings(onDeadlock = action)),
      ThreadSampler(5),
      List(DeadlockCheck()),
      LoggingFindingReporter(1.minute, 10, Clock.systemUTC),
      indicator,
      health,
      fatalErrors,
      Clock.systemUTC,
      IORuntime.global
    )
}
