package dev.coilix.diagnostics

import java.lang.management.ManagementFactory
import java.lang.management.ThreadInfo
import java.lang.management.ThreadMXBean
import java.time.Instant
import scala.jdk.CollectionConverters.*

/**
 * Takes a [[dev.coilix.diagnostics.ThreadSample]] of the platform threads.
 *
 * The state of a thread comes from `Thread.getState` and its stack from `Thread.getAllStackTraces`. Only the lock a thread
 * waits for and its owner come from `ThreadMXBean`, which on JDK 25 and newer reports a thread waiting for a monitor of a
 * virtual thread as `RUNNABLE` without a lock (ADR 0039).
 *
 * @param stackDepth
 *   how many frames of each stack are kept
 */
final class ThreadSampler(stackDepth: Int) {

  private val management: ThreadMXBean = ManagementFactory.getThreadMXBean

  /** The platform threads of the JVM now, stamped with `at`. */
  def sample(at: Instant): ThreadSample = {
    val stacks = Thread.getAllStackTraces.asScala.toList
    val locks  = lockInfo(stacks.map(_._1.getId))
    ThreadSample(
      at,
      management.getThreadCount,
      management.getDaemonThreadCount,
      stacks.map((thread, stack) => snapshotOf(thread, stack, locks.get(thread.getId))),
      Option(management.findDeadlockedThreads()).fold(Set.empty[Long])(_.toSet)
    )
  }

  private def lockInfo(ids: List[Long]): Map[Long, ThreadInfo] =
    management.getThreadInfo(ids.toArray, 0).toList.flatMap(Option(_)).map(info => info.getThreadId -> info).toMap

  private def snapshotOf(thread: Thread, stack: Array[StackTraceElement], info: Option[ThreadInfo]): ThreadSnapshot =
    ThreadSnapshot(
      thread.getId,
      thread.getName,
      thread.getState,
      thread.isDaemon,
      stack.toList.take(stackDepth),
      info.flatMap(lock => Option(lock.getLockName)),
      info.flatMap(lock => Option(lock.getLockOwnerName))
    )
}
