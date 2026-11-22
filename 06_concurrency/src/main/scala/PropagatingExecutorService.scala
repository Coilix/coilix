package dev.coilix.concurrency

import java.util.List as JavaList
import java.util.concurrent.AbstractExecutorService
import java.util.concurrent.ExecutorService
import java.util.concurrent.TimeUnit

/**
 * An `ExecutorService` that captures the context of `propagators` on the submitting thread and restores it around each task.
 * Everything else goes to `underlying`, so a worker that submits a task still hands it to its own queue.
 */
private[concurrency] final class PropagatingExecutorService(underlying: ExecutorService, propagators: List[ContextPropagator])
    extends AbstractExecutorService {

  override def execute(task: Runnable): Unit = {
    val snapshot = ContextPropagator.captureAll(propagators)
    underlying.execute(() => snapshot.runWith(task.run()))
  }

  override def shutdown(): Unit = underlying.shutdown()

  override def shutdownNow(): JavaList[Runnable] = underlying.shutdownNow()

  override def isShutdown: Boolean = underlying.isShutdown

  override def isTerminated: Boolean = underlying.isTerminated

  override def awaitTermination(timeout: Long, unit: TimeUnit): Boolean = underlying.awaitTermination(timeout, unit)
}

/** Wrapping of executor services in [[dev.coilix.concurrency.PropagatingExecutorService]]. */
private[concurrency] object PropagatingExecutorService {

  /** `service` itself when there is nothing to propagate, so tasks pay nothing for it. */
  def wrap(service: ExecutorService, propagators: List[ContextPropagator]): ExecutorService =
    if (propagators.isEmpty) service else PropagatingExecutorService(service, propagators)
}
