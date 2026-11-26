package dev.coilix.demo
package access

import cats.effect.IO
import dev.coilix.annotation.Component
import dev.coilix.annotation.Slf4s
import dev.coilix.annotation.Value
import dev.coilix.concurrency.TaskScheduler
import dev.coilix.context.ApplicationContext
import dev.coilix.context.ClosingPhase
import dev.coilix.context.ContextLifecycleListener
import scala.concurrent.duration.FiniteDuration

/**
 * Makes the [[dev.coilix.demo.access.InMemoryReplayGuard]] forget old requests every `workshop.replay-window`.
 *
 * It schedules the work from code with the [[dev.coilix.concurrency.TaskScheduler]], because the window comes from the
 * configuration. The sweeping brings new work in, so it stops in the [[dev.coilix.context.ClosingPhase.Producers]] phase,
 * together with the `@Scheduled` methods.
 */
@Component
@Slf4s
final class ClaimSweeper(
  guard:                                          InMemoryReplayGuard,
  scheduler:                                      TaskScheduler,
  @Value("${workshop.replay-window:10m}") window: FiniteDuration
) extends ContextLifecycleListener {
  private val sweep = scheduler.scheduleWithFixedDelay(window, window) {
    log.info(s"Replay guard forgot ${guard.forget()} requests")
  }

  /** Whether the sweeping still runs. */
  def isSweeping: Boolean = !sweep.isCancelled

  override def closingPhase: ClosingPhase = ClosingPhase.Producers

  override def onContextStarted(context: ApplicationContext): IO[Unit] = IO.unit

  override def onContextClosing: IO[Unit] = IO(sweep.cancel())
}
