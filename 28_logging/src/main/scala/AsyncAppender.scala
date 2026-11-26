package dev.coilix.logging

import java.util.concurrent.ArrayBlockingQueue
import java.util.concurrent.BlockingQueue
import java.util.concurrent.TimeUnit
import scala.annotation.tailrec

/**
 * Hands events to the platform thread `coilix-logging`, which writes them to `delegate`, so logging does not wait for the
 * console. When `capacity` events already wait, the event is written by the thread that logs instead of being dropped.
 *
 * The thread is a daemon, and [[dev.coilix.logging.AsyncAppender.close]] writes the waiting events before it returns. This
 * thread is not part of the pools of Coilix because it must keep writing while the pools shut down.
 */
case class AsyncAppender(delegate: LogAppender, queue: BlockingQueue[Option[LogEvent]], writer: Thread) extends LogAppender {

  override def append(event: LogEvent): Unit =
    if (!queue.offer(Some(event))) delegate.append(event)

  override def close(): Unit = {
    queue.put(None)
    writer.join(TimeUnit.SECONDS.toMillis(5))
    delegate.close()
  }
}

/** Creation of the [[dev.coilix.logging.AsyncAppender]]. */
object AsyncAppender {

  /** An appender whose writer thread has started. */
  def apply(delegate: LogAppender, capacity: Int): AsyncAppender = {
    val queue  = ArrayBlockingQueue[Option[LogEvent]](math.max(capacity, 1))
    val writer = Thread(() => drain(queue, delegate), "coilix-logging")
    writer.setDaemon(true)
    writer.start()
    AsyncAppender(delegate, queue, writer)
  }

  @tailrec
  private def drain(queue: BlockingQueue[Option[LogEvent]], delegate: LogAppender): Unit =
    queue.take() match {
      case Some(event) =>
        delegate.append(event)
        drain(queue, delegate)
      case None        => ()
    }
}
