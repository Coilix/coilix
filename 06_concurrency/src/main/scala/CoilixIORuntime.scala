package dev.coilix.concurrency

import cats.effect.unsafe.IORuntime
import dev.coilix.annotation.PreDestroy
import dev.coilix.annotation.Slf4s
import java.util.concurrent.atomic.AtomicBoolean

/**
 * Owns the cats-effect runtime of the application: creates it and shuts it down when the context closes.
 *
 * The runtime has its own work-stealing compute pool of `threads` threads named `io-compute-<number>`, which also runs its
 * timers and I/O polling, and `IO.blocking` blocks in place on helper threads, as cats-effect does by default. cats-effect shuts
 * its runtime down by itself after a fatal error in a fiber; such a shutdown, which the context did not ask for, goes to
 * `fatalErrors` as an [[dev.coilix.concurrency.IORuntimeStoppedException]].
 *
 * Its flags are atomics, because it runs on the thread that closes the context and on the threads of the runtime it owns,
 * also after that runtime has stopped running fibers.
 *
 * @param threads
 *   the number of compute threads
 * @param fatalErrors
 *   receives a shutdown of the runtime that the context did not ask for
 */
@Slf4s
@SuppressWarnings(Array("dev.coilix.warts.ThreadPrimitives"))
final class CoilixIORuntime(threads: Int, fatalErrors: FatalErrorHandler) {

  private val closing = AtomicBoolean(false)

  private val stoppedUnasked = AtomicBoolean(false)

  /** The runtime, ready to run `IO`. */
  val runtime: IORuntime = {
    val (pool, poller, shutdownPool) = IORuntime.createWorkStealingComputeThreadPool(threads = threads)
    IORuntime
      .builder()
      .setCompute(pool, shutdownPool)
      .addPoller(poller, () => ())
      .addShutdownHook(() => stopped())
      .build()
  }

  log.info(s"cats-effect runtime computes on $threads io-compute threads, the only compute pool of the application")

  /** Whether the runtime was shut down, by the context or after a fatal error. */
  def isShutDown: Boolean = closing.get || stoppedUnasked.get

  /** Shuts the runtime down when the context closes. Closing it again does nothing. */
  @PreDestroy
  def close(): Unit =
    if (closing.compareAndSet(false, true)) runtime.shutdown()

  private def stopped(): Unit =
    if (!closing.get && stoppedUnasked.compareAndSet(false, true)) {
      fatalErrors.handle(Thread.currentThread, IORuntimeStoppedException())
    }
}
