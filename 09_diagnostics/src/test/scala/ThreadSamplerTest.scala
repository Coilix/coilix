package dev.coilix.diagnostics

import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import munit.FunSuite

final class ThreadSamplerTest extends FunSuite {

  test("sees a platform thread blocked on a monitor with the lock and its owner") {
    val monitor = new AnyRef
    val entered = CountDownLatch(1)
    val waiter  = Thread(() => monitor.synchronized(entered.countDown()), "sampler-waiter")
    monitor.synchronized {
      waiter.start()
      waitUntilBlocked(waiter, 100)
      val seen = ThreadSampler(5).sample(Samples.start).threads.find(_.name == "sampler-waiter")
      assertEquals(seen.map(t => (t.state, t.lockOwner)), Some((Thread.State.BLOCKED, Some(Thread.currentThread.getName))))
      assert(seen.exists(_.lock.exists(_.startsWith("java.lang.Object@"))), seen)
    }
    assert(entered.await(5, TimeUnit.SECONDS))
  }

  test("reports deadlocked platform threads and keeps the stack depth") {
    val deadlock = LockDeadlock("sampler-deadlock")
    try {
      deadlock.awaitDeadlock()
      val sample   = ThreadSampler(3).sample(Samples.start)
      val involved =
        sample.threads.filter(thread => sample.deadlocked.contains(thread.id) && thread.name.startsWith("sampler-deadlock"))
      assertEquals(involved.map(_.name).sorted, deadlock.names)
      assert(involved.forall(_.stack.size <= 3))
      assert(sample.threadCount >= 2)
    } finally {
      deadlock.close()
    }
  }

  private def waitUntilBlocked(thread: Thread, attempts: Int): Unit =
    if (attempts > 0 && thread.getState != Thread.State.BLOCKED) {
      Thread.sleep(10)
      waitUntilBlocked(thread, attempts - 1)
    }
}
