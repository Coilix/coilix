package dev.coilix.pekko

import java.util.Collections
import java.util.List as JList
import java.util.concurrent.AbstractExecutorService
import java.util.concurrent.RejectedExecutionException
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicBoolean

/**
 * An `ExecutorService` view of an executor that belongs to Coilix, given to a dispatcher of Pekko.
 *
 * Shutting the view down only stops it from accepting tasks; the executor behind it keeps running, because the context closes
 * it after the `ActorSystem`.
 *
 * @param run
 *   runs one task on the executor of Coilix
 */
final class SharedExecutorService(run: Runnable => Unit) extends AbstractExecutorService {
  private val stopped = AtomicBoolean(false)

  override def execute(command: Runnable): Unit =
    if (stopped.get) throw RejectedExecutionException("The dispatcher of Pekko is shut down")
    else run(command)

  override def shutdown(): Unit = stopped.set(true)

  override def shutdownNow(): JList[Runnable] = {
    stopped.set(true)
    Collections.emptyList()
  }

  override def isShutdown: Boolean = stopped.get

  override def isTerminated: Boolean = stopped.get

  override def awaitTermination(timeout: Long, unit: TimeUnit): Boolean = stopped.get
}
