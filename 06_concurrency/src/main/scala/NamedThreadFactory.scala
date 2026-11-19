package dev.coilix.concurrency

import java.util.concurrent.ThreadFactory
import java.util.concurrent.atomic.AtomicLong

/**
 * Creates daemon platform threads named `<prefix><number>`, numbered from 1, such as `coilix-scheduler-1`.
 *
 * The threads are daemons, so they do not keep the JVM running. Coilix stops them when the context closes.
 *
 * @param prefix
 *   the beginning of each thread name
 */
final class NamedThreadFactory(prefix: String) extends ThreadFactory {

  private val counter = AtomicLong()

  override def newThread(task: Runnable): Thread = {
    val thread = Thread(task, s"$prefix${counter.incrementAndGet()}")
    thread.setDaemon(true)
    thread
  }
}
