package dev.coilix.processing

import dev.coilix.concurrency.FatalErrorHandler
import dev.coilix.concurrency.FatalErrors
import dev.coilix.concurrency.NamedThreadFactory
import java.util.concurrent.ExecutionException
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import java.util.concurrent.RejectedExecutionException
import java.util.concurrent.ThreadFactory
import java.util.concurrent.atomic.AtomicReference
import scala.util.control.NonFatal

/**
 * One thread that runs the operations of the processor manager in order, so the state of the manager changes without
 * locks.
 *
 * An operation called from the thread itself, such as an operation of the manager in a callback of a processor, runs at
 * once instead of waiting behind itself.
 */
private[processing] final class ControlThread(name: String, fatalErrors: FatalErrorHandler) extends AutoCloseable {

  private val owner = AtomicReference(Option.empty[Thread])

  private val executor: ExecutorService = Executors.newSingleThreadExecutor(capturing(NamedThreadFactory(name, None)))

  /** Runs the task on the thread and waits for its result, rethrowing its exception. */
  def call[T](task: => T): T =
    if (owner.get.contains(Thread.currentThread)) task
    else awaited(executor.submit(() => task))

  /** Runs the task on the thread later. Tasks posted after closing are dropped. */
  def post(task: => Unit): Unit =
    try {
      executor.execute(() => FatalErrors.guarded(name, fatalErrors)(task))
    } catch {
      case _: RejectedExecutionException => ()
    }

  override def close(): Unit = executor.shutdownNow(): Unit

  private def awaited[T](result: java.util.concurrent.Future[T]): T =
    try {
      result.get
    } catch {
      case error: ExecutionException => throw Option(error.getCause).filter(NonFatal(_)).getOrElse(error)
    }

  private def capturing(factory: ThreadFactory): ThreadFactory =
    task => {
      val thread = factory.newThread(task)
      thread.setName(name)
      owner.set(Option(thread))
      thread
    }
}
