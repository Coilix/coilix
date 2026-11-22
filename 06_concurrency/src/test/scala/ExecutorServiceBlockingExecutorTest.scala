package dev.coilix.concurrency

import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicInteger
import munit.FunSuite
import scala.concurrent.Await
import scala.concurrent.duration.DurationInt

final class ExecutorServiceBlockingExecutorTest extends FunSuite {

  test("runs each task on a virtual thread when the JDK has them") {
    withExecutor(BlockingSettings(threadNamePrefix = "io-")) { executor =>
      val thread = Await.result(executor.submit(Thread.currentThread), 5.seconds)
      assertEquals(
        (thread.getName.startsWith("io-"), ThreadFactories.isVirtual(thread)),
        (true, ThreadFactories.virtualThreadsAvailable)
      )
    }
  }

  test("runs tasks on platform threads when virtual threads are turned off") {
    withExecutor(BlockingSettings(virtualThreads = false, platformThreads = 1, threadNamePrefix = "io-")) { executor =>
      val threads = (1 to 3).toList.map(_ => Await.result(executor.submit(Thread.currentThread), 5.seconds))
      assertEquals(threads.map(thread => (thread.getName, ThreadFactories.isVirtual(thread))).distinct, List(("io-1", false)))
    }
  }

  test("runs at most max-concurrency tasks at once") {
    withExecutor(BlockingSettings(maxConcurrency = Some(2))) { executor =>
      val running = AtomicInteger()
      val peak    = AtomicInteger()
      val done    = CountDownLatch(8)
      (1 to 8).foreach { _ =>
        executor.execute {
          peak.accumulateAndGet(running.incrementAndGet(), Math.max)
          Thread.sleep(50)
          running.decrementAndGet()
          done.countDown()
        }
      }
      assert(done.await(10, TimeUnit.SECONDS))
      assertEquals((peak.get, executor.availablePermits), (2, Some(2)))
    }
  }

  test("has no limit without max-concurrency") {
    withExecutor(BlockingSettings()) { executor =>
      assertEquals(executor.availablePermits, None)
    }
  }

  private def withExecutor(settings: BlockingSettings)(body: ExecutorServiceBlockingExecutor => Unit): Unit = {
    val executor = ExecutorServiceBlockingExecutor(settings.copy(shutdownTimeout = 5.seconds))
    try {
      body(executor)
    } finally {
      executor.close()
    }
  }
}
