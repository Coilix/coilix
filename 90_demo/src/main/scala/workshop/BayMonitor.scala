package dev.coilix.demo
package workshop

import dev.coilix.annotation.Component
import dev.coilix.annotation.Scheduled
import dev.coilix.annotation.Slf4s
import dev.coilix.annotation.Value
import dev.coilix.cluster.ReplicaIdentity
import java.util.concurrent.atomic.AtomicInteger

/**
 * Checks the workshop bays on the Coilix task scheduler, every `workshop.bay-check-interval` (one second by default).
 *
 * With several replicas of the demo, each replica checks its own share of the `workshop.bays` bays, chosen by the
 * [[dev.coilix.cluster.ReplicaIdentity]].
 *
 * The log line shows the scheduler thread, such as `coilix-scheduler-1`. The demo stops after its runners, so the check
 * usually runs once.
 */
@Component
@Slf4s
final class BayMonitor(identity: ReplicaIdentity, @Value("${workshop.bays:4}") bays: Int) {
  private val checks = AtomicInteger()

  /** The bays this replica checks. */
  def myBays: List[Int] = (1 to bays).toList.filter(_ % identity.replicaCount == identity.self.index)

  /** The number of checks so far. */
  def checksDone: Int = checks.get

  @Scheduled(fixedRate = "${workshop.bay-check-interval:1s}")
  private def checkBays(): Unit = {
    val check = checks.incrementAndGet()
    log.info(
      s"Bay check $check on thread ${Thread.currentThread.getName}, replica ${identity.self.index} of ${identity.replicaCount}: " +
        s"bays ${myBays.mkString(", ")} are free"
    )
  }
}
