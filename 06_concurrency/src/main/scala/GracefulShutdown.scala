package dev.coilix.concurrency

import dev.coilix.annotation.Slf4s
import java.util.concurrent.ExecutorService
import java.util.concurrent.TimeUnit
import scala.concurrent.duration.FiniteDuration

/** Stops executor services without losing tasks silently. */
@Slf4s
object GracefulShutdown {

  /**
   * Stops accepting tasks, waits up to `timeout` for running and queued tasks, then interrupts the rest and logs at `WARN`
   * level how many tasks did not finish. Does nothing when `service` is already stopped.
   *
   * @param name
   *   the executor name used in log messages
   */
  def close(name: String, service: ExecutorService, timeout: FiniteDuration): Unit =
    if (!service.isShutdown) {
      service.shutdown()
      if (!service.awaitTermination(timeout.toMillis, TimeUnit.MILLISECONDS)) interrupt(name, service, timeout)
    }

  private def interrupt(name: String, service: ExecutorService, timeout: FiniteDuration): Unit = {
    val dropped = service.shutdownNow().size
    log.warn(s"$name did not finish its tasks within $timeout, interrupted the running ones and dropped $dropped queued")
  }
}
