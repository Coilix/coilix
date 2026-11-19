package dev.coilix.concurrency

import java.util.concurrent.ScheduledFuture

/**
 * A [[dev.coilix.concurrency.ScheduledTask]] backed by the `ScheduledFuture` of a JDK scheduled executor.
 *
 * @param future
 *   the handle returned by the executor
 */
final class FutureScheduledTask(future: ScheduledFuture[?]) extends ScheduledTask {

  override def cancel(): Unit = {
    val _ = future.cancel(false)
  }

  override def isCancelled: Boolean = future.isCancelled

  override def isDone: Boolean = future.isDone
}
