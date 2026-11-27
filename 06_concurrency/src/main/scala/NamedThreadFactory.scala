package dev.coilix.concurrency

import java.util.concurrent.ThreadFactory
import java.util.concurrent.atomic.AtomicLong

/**
 * Creates daemon platform threads named `<prefix><number>`, numbered from 1, such as `coilix-diagnostics-1`.
 *
 * The threads are daemons, so they do not keep the JVM running. Coilix stops them when the context closes.
 *
 * @param prefix
 *   the beginning of each thread name
 * @param uncaught
 *   receives the exceptions that end a thread, such as fatal errors; without it the JVM default applies
 */
final class NamedThreadFactory(prefix: String, uncaught: Option[Thread.UncaughtExceptionHandler] = None) extends ThreadFactory {

  private val counter = AtomicLong()

  override def newThread(task: Runnable): Thread = {
    val thread = Thread(task, s"$prefix${counter.incrementAndGet()}")
    thread.setDaemon(true)
    uncaught.foreach(thread.setUncaughtExceptionHandler)
    thread
  }
}
