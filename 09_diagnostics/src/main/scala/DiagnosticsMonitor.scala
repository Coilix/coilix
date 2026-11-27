package dev.coilix.diagnostics

import cats.effect.IO
import cats.effect.unsafe.IORuntime
import dev.coilix.annotation.Slf4s
import dev.coilix.concurrency.FatalErrorHandler
import dev.coilix.context.ApplicationContext
import dev.coilix.context.ContextLifecycleListener
import dev.coilix.context.SynchronousLifecycle
import java.time.Clock
import java.util.concurrent.Executors
import java.util.concurrent.ScheduledExecutorService
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicReference
import scala.util.control.NonFatal

/**
 * Runs the diagnostics of the application on its own platform thread, `coilix-diagnostics`, outside the pools of the
 * application, so that it keeps working when they are stuck.
 *
 * Every `coilix.diagnostics.interval` it takes a [[dev.coilix.diagnostics.ThreadSample]], runs every
 * [[dev.coilix.diagnostics.ThreadCheck]], hands the findings to the [[dev.coilix.diagnostics.FindingReporter]] and refreshes
 * the [[dev.coilix.diagnostics.Health]]. A deadlock finding leads to the reaction set by
 * `coilix.diagnostics.threads.on-deadlock`. The first round runs when the context has started, on the starting thread; the
 * diagnostics thread then runs the next ones until the context starts closing.
 *
 * A failed round is logged and the next one runs as usual. Only a `VirtualMachineError`, such as an `OutOfMemoryError`, goes
 * to the [[dev.coilix.concurrency.FatalErrorHandler]]: a `LinkageError` here comes from the code of the diagnostics or of a
 * check, so it does not mean that the application is broken, and the monitor must not end the application because it cannot
 * observe it.
 *
 * @param indicator
 *   the `threads` liveness indicator, set to `Down` after a deadlock in the `report` mode
 * @param fatalErrors
 *   gets a [[dev.coilix.diagnostics.ThreadDeadlockException]] after a deadlock in the `shutdown` mode
 */
@Slf4s
final class DiagnosticsMonitor(
  settings:    DiagnosticsSettings,
  sampler:     ThreadSampler,
  checks:      List[ThreadCheck],
  reporter:    FindingReporter,
  indicator:   SettableHealthIndicator,
  health:      Health,
  fatalErrors: FatalErrorHandler,
  clock:       Clock,
  runtime:     IORuntime
) extends ContextLifecycleListener {

  private val history   = AtomicReference(ThreadHistory.empty)
  private val reacted   = AtomicBoolean(false)
  private val scheduler = AtomicReference(Option.empty[ScheduledExecutorService])

  override def onContextStarted(context: ApplicationContext): IO[Unit] = IO.blocking {
    round()
    val executor = Executors.newSingleThreadScheduledExecutor(runnable => diagnosticsThread(runnable))
    val period   = settings.interval.toMillis
    val _        = executor.scheduleWithFixedDelay(() => round(), period, period, TimeUnit.MILLISECONDS)
    scheduler.set(Some(executor))
  }

  override def onContextClosing: IO[Unit] =
    IO(scheduler.getAndSet(None).foreach(_.shutdownNow()))

  /** Runs one round of diagnostics now and returns its findings; the diagnostics thread calls it every interval. */
  def runChecks(): List[Finding] = {
    val findings = if (settings.threads.enabled) threadFindings() else Nil
    reporter.report(findings)
    findings.find(_.kind == Finding.deadlock).foreach(react)
    SynchronousLifecycle.runOn(runtime, health.refresh())
    findings
  }

  /** Runs one round on behalf of the diagnostics thread, which logs a failure and keeps running. */
  private[diagnostics] def round(): Unit =
    try {
      val _ = runChecks()
    } catch {
      case NonFatal(failure) => log.error("A round of diagnostics failed", failure)
      case failure: LinkageError        => log.error("A round of diagnostics failed, the diagnostics code cannot be loaded", failure)
      case failure: VirtualMachineError => fatalErrors.handle(Thread.currentThread, failure)
    }

  private def threadFindings(): List[Finding] = {
    val sample = sampler.sample(clock.instant)
    val seen   = history.updateAndGet(_.next(sample, settings.threads.growthWindow))
    checks.flatMap(check => findingsOf(check, sample, seen))
  }

  private def findingsOf(check: ThreadCheck, sample: ThreadSample, seen: ThreadHistory): List[Finding] =
    try {
      check.check(sample, seen)
    } catch {
      case NonFatal(failure) =>
        log.warn(s"Thread check ${check.getClass.getName} failed", failure)
        Nil
    }

  private def react(deadlock: Finding): Unit =
    settings.threads.onDeadlock match {
      case DeadlockAction.Report   => indicator.set(HealthState.down(deadlock.message))
      case DeadlockAction.Shutdown =>
        if (reacted.compareAndSet(false, true)) fatalErrors.handle(Thread.currentThread, ThreadDeadlockException(deadlock.message))
    }

  private def diagnosticsThread(runnable: Runnable): Thread = {
    val thread = Thread(runnable, "coilix-diagnostics")
    thread.setDaemon(true)
    thread
  }
}
