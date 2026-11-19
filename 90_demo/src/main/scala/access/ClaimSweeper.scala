package dev.coilix.demo
package access

import dev.coilix.annotation.Component
import dev.coilix.annotation.PreDestroy
import dev.coilix.annotation.Slf4s
import dev.coilix.annotation.Value
import dev.coilix.concurrency.TaskScheduler
import scala.concurrent.duration.FiniteDuration

/**
 * Makes the [[dev.coilix.demo.access.InMemoryReplayGuard]] forget old requests every `workshop.replay-window`.
 *
 * It schedules the work from code with the [[dev.coilix.concurrency.TaskScheduler]], because the window comes from the
 * configuration, and cancels it when the context closes.
 */
@Component
@Slf4s
final class ClaimSweeper(
  guard:                                          InMemoryReplayGuard,
  scheduler:                                      TaskScheduler,
  @Value("${workshop.replay-window:10m}") window: FiniteDuration
) {
  private val sweep = scheduler.scheduleWithFixedDelay(window, window) {
    log.info(s"Replay guard forgot ${guard.forget()} requests")
  }

  /** Stops the sweeping. */
  @PreDestroy
  def stop(): Unit = sweep.cancel()
}
