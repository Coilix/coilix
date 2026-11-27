package dev.coilix.diagnostics

import cats.effect.IO
import cats.effect.Ref
import cats.syntax.all.*
import dev.coilix.annotation.Slf4s
import dev.coilix.concurrency.FatalErrorHandler
import dev.coilix.context.ApplicationContext
import dev.coilix.context.ContextLifecycleListener
import java.time.Clock
import java.util.concurrent.Executors
import java.util.concurrent.ScheduledExecutorService
import java.util.concurrent.TimeUnit
import scala.concurrent.ExecutionContext
import scala.concurrent.duration.DurationInt
import scala.util.control.NonFatal

/**
 * Runs the diagnostics of the application on its own platform thread, `coilix-diagnostics`, outside the pools of the
 * application, so that it keeps working when they are stuck.
 *
 * Every `coilix.diagnostics.interval` it takes a [[dev.coilix.diagnostics.ThreadSample]], runs every
 * [[dev.coilix.diagnostics.ThreadCheck]], hands the findings to the [[dev.coilix.diagnostics.FindingReporter]] and refreshes
 * the [[dev.coilix.diagnostics.Health]]. A deadlock finding leads to the reaction set by
 * `coilix.diagnostics.threads.on-deadlock`. The first round runs when the context has started; the next ones run in a fiber
 * shifted to the diagnostics thread, which also wakes it between rounds, so neither the rounds nor the waits need a thread of
 * the compute pool. The fiber is cancelled when the context starts closing.
 *
 * A failed round is logged and the next one runs as usual. Only a `VirtualMachineError`, such as an `OutOfMemoryError`, thrown
 * while sampling or checking goes to the [[dev.coilix.concurrency.FatalErrorHandler]]: a `LinkageError` here comes from the
 * code of the diagnostics or of a check, so it does not mean that the application is broken, and the monitor must not end the
 * application because it cannot observe it.
 *
 * @param indicator
 *   the `threads` liveness indicator, set to `Down` after a deadlock in the `report` mode
 * @param fatalErrors
 *   gets a [[dev.coilix.diagnostics.ThreadDeadlockException]] after a deadlock in the `shutdown` mode
 * @param history
 *   the thread counts of the earlier rounds
 * @param reacted
 *   whether a deadlock was already handed to `fatalErrors`
 * @param stop
 *   stops the rounds on the diagnostics thread, once they run
 */
@Slf4s
case class DiagnosticsMonitor(
  settings:    DiagnosticsSettings,
  sampler:     ThreadSampler,
  checks:      List[ThreadCheck],
  reporter:    FindingReporter,
  indicator:   SettableHealthIndicator,
  health:      Health,
  fatalErrors: FatalErrorHandler,
  clock:       Clock,
  history:     Ref[IO, ThreadHistory],
  reacted:     Ref[IO, Boolean],
  stop:        Ref[IO, Option[IO[Unit]]]
) extends ContextLifecycleListener {

  override def onContextStarted(context: ApplicationContext): IO[Unit] =
    round *> IO(Executors.newSingleThreadScheduledExecutor(runnable => diagnosticsThread(runnable))).flatMap(started)

  override def onContextClosing: IO[Unit] =
    stop.getAndSet(None).flatMap(_.getOrElse(IO.unit))

  /** Runs one round of diagnostics and returns its findings; the diagnostics thread runs it every interval. */
  def runChecks: IO[List[Finding]] =
    for {
      findings <- if (settings.threads.enabled) threadFindings else IO.pure(Nil)
      _        <- reporter.report(findings)
      _        <- findings.find(_.kind == Finding.deadlock).traverse_(react)
      _        <- health.refresh()
    } yield findings

  /** Runs one round on behalf of the diagnostics thread, which logs a failure and keeps running. */
  private[diagnostics] def round: IO[Unit] =
    runChecks.void.handleErrorWith(failure => logF.error("A round of diagnostics failed", failure))

  private def started(executor: ScheduledExecutorService): IO[Unit] =
    rounds(executor)
      .evalOn(ExecutionContext.fromExecutorService(executor))
      .start
      .flatMap(fiber => stop.set(Some(stopped(fiber.cancel, executor))))

  private def stopped(cancel: IO[Unit], executor: ScheduledExecutorService): IO[Unit] =
    cancel.timeoutTo(5.seconds, logF.warn("A round of diagnostics did not stop in 5 seconds")) *>
      IO(executor.shutdownNow()).void

  private def rounds(executor: ScheduledExecutorService): IO[Unit] = (pause(executor) *> round).foreverM

  private def pause(executor: ScheduledExecutorService): IO[Unit] =
    IO.async { resume =>
      IO {
        val wake = executor.schedule((() => resume(Right(()))): Runnable, settings.interval.toMillis, TimeUnit.MILLISECONDS)
        Some(IO(wake.cancel(false)).void)
      }
    }

  private def threadFindings: IO[List[Finding]] =
    IO(sampler.sample(clock.instant)).flatMap { sample =>
      history.updateAndGet(_.next(sample, settings.threads.growthWindow)).flatMap(seen => IO(checked(sample, seen)))
    }

  private def checked(sample: ThreadSample, seen: ThreadHistory): List[Finding] =
    try {
      checks.flatMap(check => findingsOf(check, sample, seen))
    } catch {
      case failure: LinkageError        =>
        log.error("A round of diagnostics failed, the diagnostics code cannot be loaded", failure)
        Nil
      case failure: VirtualMachineError =>
        fatalErrors.handle(Thread.currentThread, failure)
        Nil
    }

  private def findingsOf(check: ThreadCheck, sample: ThreadSample, seen: ThreadHistory): List[Finding] =
    try {
      check.check(sample, seen)
    } catch {
      case NonFatal(failure) =>
        log.warn(s"Thread check ${check.getClass.getName} failed", failure)
        Nil
    }

  private def react(deadlock: Finding): IO[Unit] =
    settings.threads.onDeadlock match {
      case DeadlockAction.Report   => indicator.set(HealthState.down(deadlock.message))
      case DeadlockAction.Shutdown => reacted.getAndSet(true).flatMap(done => IO.unlessA(done)(shutdown(deadlock)))
    }

  private def shutdown(deadlock: Finding): IO[Unit] =
    IO(fatalErrors.handle(Thread.currentThread, ThreadDeadlockException(deadlock.message)))

  private def diagnosticsThread(runnable: Runnable): Thread = {
    val thread = Thread(runnable, "coilix-diagnostics")
    thread.setDaemon(true)
    thread
  }
}

/** Creation of the [[dev.coilix.diagnostics.DiagnosticsMonitor]]. */
object DiagnosticsMonitor {

  /** A monitor that has run no round yet. Creating the `Ref`s runs no effect, so the monitor is a plain bean. */
  def apply(
    settings:    DiagnosticsSettings,
    sampler:     ThreadSampler,
    checks:      List[ThreadCheck],
    reporter:    FindingReporter,
    indicator:   SettableHealthIndicator,
    health:      Health,
    fatalErrors: FatalErrorHandler,
    clock:       Clock
  ): DiagnosticsMonitor =
    DiagnosticsMonitor(
      settings,
      sampler,
      checks,
      reporter,
      indicator,
      health,
      fatalErrors,
      clock,
      Ref.unsafe[IO, ThreadHistory](ThreadHistory.empty),
      Ref.unsafe[IO, Boolean](false),
      Ref.unsafe[IO, Option[IO[Unit]]](None)
    )
}
