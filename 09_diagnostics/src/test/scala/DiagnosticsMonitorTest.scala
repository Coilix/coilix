package dev.coilix.diagnostics

import cats.effect.IO
import cats.effect.Resource
import cats.syntax.all.*
import java.time.Clock
import munit.CatsEffectSuite
import scala.concurrent.duration.DurationInt

final class DiagnosticsMonitorTest extends CatsEffectSuite {

  test("hands a deadlock to the fatal error handler once in the shutdown mode") {
    val fatalErrors = RecordingFatalErrorHandler()
    val monitor     = monitorOf(DeadlockAction.Shutdown, fatalErrors, SettableHealthIndicator("threads", HealthKind.Liveness))
    deadlock("shutdown-deadlock").use { _ =>
      (monitor.runChecks <* monitor.runChecks).map { findings =>
        assertEquals(findings.filter(_.kind == Finding.deadlock).map(_.severity), List(FindingSeverity.Critical))
        assertEquals(fatalErrors.failures.map(_.getClass), List(classOf[ThreadDeadlockException]))
        assert(fatalErrors.failures.head.getMessage.contains("shutdown-deadlock-1"), fatalErrors.failures.head.getMessage)
      }
    }
  }

  test("sets the threads indicator Down and fails liveness in the report mode") {
    val indicator   = SettableHealthIndicator("threads", HealthKind.Liveness)
    val health      = DefaultHealth(List(indicator), RecordingPublisher())
    val fatalErrors = RecordingFatalErrorHandler()
    val monitor     = monitorOf(DeadlockAction.Report, fatalErrors, indicator, health)
    deadlock("report-deadlock").use { _ =>
      for {
        _        <- monitor.runChecks
        liveness <- health.liveness
        state    <- indicator.state
      } yield {
        assertEquals((liveness, fatalErrors.failures), (HealthStatus.Down, Nil))
        assert(state.reason.contains("report-deadlock-2"), state.reason)
      }
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
      Clock.systemUTC
    )
    (memory.set(HealthState.degraded("memory is short")) *> monitor.runChecks, health.readiness).tupled
      .assertEquals((Nil, HealthStatus.Degraded))
  }

  test("a failing check is skipped and the other checks still run") {
    val failing = new ThreadCheck {
      override def check(sample: ThreadSample, history: ThreadHistory): List[Finding] = throw IllegalStateException("broken")
    }
    monitorWith(List(failing, ThreadCountCheck(1, Int.MaxValue)), RecordingFatalErrorHandler()).runChecks
      .map(_.map(_.kind))
      .assertEquals(List(Finding.tooManyThreads))
  }

  test("a round survives a LinkageError of a check and hands only a VirtualMachineError to the fatal error handler") {
    val fatalErrors = RecordingFatalErrorHandler()
    def throwing(failure: Throwable): DiagnosticsMonitor = monitorWith(List((_, _) => throw failure), fatalErrors)
    (throwing(NoClassDefFoundError("dev/coilix/diagnostics/Missing")).round *> throwing(OutOfMemoryError("test")).round)
      .map(_ => assertEquals(fatalErrors.failures.map(_.getClass), List(classOf[OutOfMemoryError])))
  }

  test("a failing reporter fails only its round") {
    val reporter = new FindingReporter {
      override def report(findings: List[Finding]): IO[Unit] = IO.raiseError(IllegalStateException("no alerting"))
    }
    val monitor  = DiagnosticsMonitor(
      DiagnosticsSettings(),
      ThreadSampler(5),
      Nil,
      reporter,
      SettableHealthIndicator("threads", HealthKind.Liveness),
      DefaultHealth(Nil, RecordingPublisher()),
      RecordingFatalErrorHandler(),
      Clock.systemUTC
    )
    monitor.round.assertEquals(())
  }

  private def deadlock(prefix: String): Resource[IO, LockDeadlock] =
    Resource.make(IO.blocking { val locks = LockDeadlock(prefix); locks.awaitDeadlock(); locks })(locks =>
      IO.blocking(locks.close())
    )

  private def monitorWith(checks: List[ThreadCheck], fatalErrors: RecordingFatalErrorHandler): DiagnosticsMonitor =
    DiagnosticsMonitor(
      DiagnosticsSettings(),
      ThreadSampler(5),
      checks,
      LoggingFindingReporter(1.minute, 10, Clock.systemUTC),
      SettableHealthIndicator("threads", HealthKind.Liveness),
      DefaultHealth(Nil, RecordingPublisher()),
      fatalErrors,
      Clock.systemUTC
    )

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
      Clock.systemUTC
    )
}
