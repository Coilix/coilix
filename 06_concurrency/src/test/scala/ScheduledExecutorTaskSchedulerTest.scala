package dev.coilix.concurrency

import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicInteger
import java.util.concurrent.atomic.AtomicReference
import munit.FunSuite
import scala.concurrent.duration.DurationInt

final class ScheduledExecutorTaskSchedulerTest extends FunSuite {
  private val scheduler = FunFixture[ScheduledExecutorTaskScheduler](
    _ => ScheduledExecutorTaskScheduler(SchedulerSettings(threadNamePrefix = "test-scheduler-")),
    _.close()
  )

  scheduler.test("runs a task once after a delay on a named thread") { scheduler =>
    val thread = AtomicReference[String]()
    val done   = CountDownLatch(1)
    val task   = scheduler.schedule(10.millis) { thread.set(Thread.currentThread.getName); done.countDown() }
    assert(done.await(5, TimeUnit.SECONDS))
    assertEquals(thread.get, "test-scheduler-1")
    assert(eventually(task.isDone))
  }

  scheduler.test("runs a task at a fixed rate") { scheduler =>
    val runs = CountDownLatch(3)
    val task = scheduler.scheduleAtFixedRate(0.millis, 10.millis)(runs.countDown())
    assert(runs.await(5, TimeUnit.SECONDS))
    task.cancel()
  }

  scheduler.test("runs a task with a fixed delay") { scheduler =>
    val runs = CountDownLatch(3)
    val task = scheduler.scheduleWithFixedDelay(0.millis, 10.millis)(runs.countDown())
    assert(runs.await(5, TimeUnit.SECONDS))
    task.cancel()
  }

  scheduler.test("keeps running a periodic task after a failed run") { scheduler =>
    val runs = AtomicInteger()
    val done = CountDownLatch(1)
    val task = scheduler.scheduleWithFixedDelay(0.millis, 10.millis) {
      if (runs.incrementAndGet() == 1) throw IllegalStateException("expected in test") else done.countDown()
    }
    assert(done.await(5, TimeUnit.SECONDS))
    task.cancel()
  }

  scheduler.test("a cancelled task does not run again") { scheduler =>
    val task = scheduler.schedule(1.hour)(())
    task.cancel()
    assertEquals((task.isCancelled, task.isDone), (true, true))
  }

  test("closing drops tasks that wait for their time") {
    val scheduler = ScheduledExecutorTaskScheduler(SchedulerSettings())
    val runs      = AtomicInteger()
    val _         = scheduler.schedule(50.millis)(runs.incrementAndGet())
    scheduler.close()
    Thread.sleep(100)
    assertEquals((runs.get, scheduler.isClosed), (0, true))
  }

  private def eventually(condition: => Boolean): Boolean =
    LazyList.range(0, 100).exists { _ =>
      condition || { Thread.sleep(10); false }
    }
}
