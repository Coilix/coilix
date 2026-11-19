package dev.coilix.demo
package workshop

import dev.coilix.annotation.Component
import dev.coilix.annotation.Scheduled
import dev.coilix.annotation.Slf4s
import java.util.concurrent.atomic.AtomicInteger

/**
 * Checks the workshop bays on the Coilix task scheduler, every `workshop.bay-check-interval` (one second by default).
 *
 * The log line shows the scheduler thread, such as `coilix-scheduler-1`. The demo stops after its runners, so the check
 * usually runs once.
 */
@Component
@Slf4s
final class BayMonitor {
  private val checks = AtomicInteger()

  /** The number of checks so far. */
  def checksDone: Int = checks.get

  @Scheduled(fixedRate = "${workshop.bay-check-interval:1s}")
  private def checkBays(): Unit = {
    val check = checks.incrementAndGet()
    log.info(s"Bay check $check on thread ${Thread.currentThread.getName}: all bays free")
  }
}
