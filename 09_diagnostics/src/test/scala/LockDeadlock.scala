package dev.coilix.diagnostics

import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import java.util.concurrent.locks.ReentrantLock

/**
 * Two platform threads deadlocked on `ReentrantLock`s. They wait interruptibly, so `close` ends the deadlock and the threads,
 * which keeps other tests in the same JVM free of it.
 */
final class LockDeadlock(prefix: String) extends AutoCloseable {

  private val first        = ReentrantLock()
  private val second       = ReentrantLock()
  private val bothHoldOne  = CountDownLatch(2)
  private val firstThread  = started(s"$prefix-1", first, second)
  private val secondThread = started(s"$prefix-2", second, first)

  /** The names of the deadlocked threads. */
  val names: List[String] = List(firstThread.getName, secondThread.getName)

  /** Waits until both threads hold their first lock and wait for the other one. */
  def awaitDeadlock(): Unit = {
    val _ = bothHoldOne.await(5, TimeUnit.SECONDS)
    waitUntilParked(50)
  }

  override def close(): Unit = {
    firstThread.interrupt()
    secondThread.interrupt()
    firstThread.join(5000)
    secondThread.join(5000)
  }

  private def waitUntilParked(attempts: Int): Unit =
    if (attempts > 0 && !(first.hasQueuedThreads && second.hasQueuedThreads)) {
      Thread.sleep(20)
      waitUntilParked(attempts - 1)
    }

  private def started(name: String, held: ReentrantLock, wanted: ReentrantLock): Thread = {
    val thread = Thread(() => holdAndWait(held, wanted), name)
    thread.setDaemon(true)
    thread.start()
    thread
  }

  private def holdAndWait(held: ReentrantLock, wanted: ReentrantLock): Unit = {
    held.lock()
    try {
      bothHoldOne.countDown()
      bothHoldOne.await()
      wanted.lockInterruptibly()
      wanted.unlock()
    } catch {
      case _: InterruptedException => ()
    } finally {
      held.unlock()
    }
  }
}
