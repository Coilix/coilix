package dev.coilix.concurrency

import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicBoolean
import munit.FunSuite
import scala.concurrent.Await
import scala.concurrent.duration.DurationInt

final class ExecutorServiceTaskExecutorTest extends FunSuite {
  private val executor = FunFixture[ExecutorServiceTaskExecutor](
    _ => ExecutorServiceTaskExecutor(ComputeSettings(threads = 2, threadNamePrefix = "test-task-", shutdownTimeout = 5.seconds)),
    _.close()
  )

  executor.test("submits a task and returns its result from a named thread") { executor =>
    val result = Await.result(executor.submit((Thread.currentThread.getName, 6 * 7)), 5.seconds)
    assertEquals((result._1.startsWith("test-task-"), result._2), (true, 42))
  }

  executor.test("computes on platform threads of a fork-join pool") { executor =>
    val thread = Await.result(executor.submit(Thread.currentThread), 5.seconds)
    assertEquals(
      (ThreadFactories.isVirtual(thread), thread.isInstanceOf[java.util.concurrent.ForkJoinWorkerThread], thread.isDaemon),
      (false, true, true)
    )
  }

  executor.test("executes a task without waiting for it") { executor =>
    val done = CountDownLatch(1)
    executor.execute(done.countDown())
    assert(done.await(5, TimeUnit.SECONDS))
  }

  executor.test("keeps running tasks after one fails") { executor =>
    val done = CountDownLatch(1)
    executor.execute(throw IllegalStateException("expected in test"))
    executor.execute(done.countDown())
    assert(done.await(5, TimeUnit.SECONDS))
  }

  executor.test("returns the failure of a task in its future") { executor =>
    val failure = intercept[IllegalStateException](Await.result(executor.submit(throw IllegalStateException("broken")), 5.seconds))
    assertEquals(failure.getMessage, "broken")
  }

  test("closing waits for started tasks") {
    val executor = ExecutorServiceTaskExecutor(ComputeSettings(shutdownTimeout = 5.seconds))
    val started  = CountDownLatch(1)
    val finished = AtomicBoolean(false)
    executor.execute { started.countDown(); Thread.sleep(100); finished.set(true) }
    assert(started.await(5, TimeUnit.SECONDS))
    executor.close()
    assertEquals((finished.get, executor.isClosed), (true, true))
  }

  test("closing interrupts tasks that do not finish in time") {
    val executor    = ExecutorServiceTaskExecutor(ComputeSettings(shutdownTimeout = 50.millis))
    val started     = CountDownLatch(1)
    val interrupted = CountDownLatch(1)
    executor.execute {
      started.countDown()
      try {
        Thread.sleep(10000)
      } catch {
        case _: InterruptedException => interrupted.countDown()
      }
    }
    assert(started.await(5, TimeUnit.SECONDS))
    executor.close()
    assert(interrupted.await(5, TimeUnit.SECONDS))
  }

  test("closing twice does nothing") {
    val executor = ExecutorServiceTaskExecutor(ComputeSettings())
    executor.close()
    executor.close()
    assert(executor.isClosed)
  }

  test("uses the configured number of threads") {
    val executor = ExecutorServiceTaskExecutor(ComputeSettings(threads = 1, threadNamePrefix = "pool-"))
    try {
      val threads = (1 to 3).toList.map(_ => Await.result(executor.submit(Thread.currentThread.getName), 5.seconds))
      assertEquals(threads.distinct, List("pool-1"))
    } finally {
      executor.close()
    }
  }
}
