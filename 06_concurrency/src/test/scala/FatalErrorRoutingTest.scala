package dev.coilix.concurrency

import dev.coilix.concurrency.fatal.RecordingFatalErrorHandler
import java.util.concurrent.TimeUnit
import munit.FunSuite
import scala.concurrent.duration.DurationInt

final class FatalErrorRoutingTest extends FunSuite {

  test("a task started by the executor passes its fatal error to the handler") {
    val handler = RecordingFatalErrorHandler()
    withExecutor(ComputeSettings(threadNamePrefix = "fatal-task-"), handler) { executor =>
      executor.execute(throw OutOfMemoryError("task"))
      assert(handler.called.await(5, TimeUnit.SECONDS))
      assertEquals(
        handler.failures.map((thread, failure) => (thread.startsWith("fatal-task-"), failure.getMessage)),
        List((true, "task"))
      )
    }
  }

  test("a fatal error that ends a thread of a submitted task reaches the handler") {
    val handler = RecordingFatalErrorHandler()
    withExecutor(ComputeSettings(threads = 1), handler) { executor =>
      val _ = executor.submit[Unit](throw StackOverflowError("submitted"))
      assert(handler.called.await(5, TimeUnit.SECONDS))
      assertEquals(handler.failures.map(_._2.getMessage), List("submitted"))
    }
  }

  test("a fatal error that ends a virtual thread of a submitted task reaches the handler") {
    val handler  = RecordingFatalErrorHandler()
    val executor = ExecutorServiceBlockingExecutor(BlockingSettings(shutdownTimeout = 5.seconds), handler)
    try {
      val _ = executor.submit[Unit](throw OutOfMemoryError("virtual"))
      assert(handler.called.await(5, TimeUnit.SECONDS))
      assertEquals(handler.failures.map(_._2.getMessage), List("virtual"))
    } finally {
      executor.close()
    }
  }

  test("a scheduled task passes its fatal error to the handler") {
    val handler   = RecordingFatalErrorHandler()
    val scheduler = ScheduledExecutorTaskScheduler(SchedulerSettings(threadNamePrefix = "fatal-scheduler-"), handler)
    try {
      val _ = scheduler.scheduleAtFixedRate(1.millis, 1.hour)(throw OutOfMemoryError("scheduled"))
      assert(handler.called.await(5, TimeUnit.SECONDS))
      assertEquals(
        handler.failures.map((thread, failure) => (thread, failure.getMessage)),
        List(("fatal-scheduler-1", "scheduled"))
      )
    } finally {
      scheduler.close()
    }
  }

  test("a thread of the factory passes an uncaught fatal error to its handler") {
    val handler = RecordingFatalErrorHandler()
    val thread  = ThreadFactories
      .named("factory-", virtual = false, Some(FatalErrors.uncaughtExceptionHandler(handler)))
      .newThread(() => throw OutOfMemoryError("thread"))
    thread.start()
    assert(handler.called.await(5, TimeUnit.SECONDS))
    assertEquals(handler.failures.map(_._1), List("factory-1"))
  }

  private def withExecutor(
    settings: ComputeSettings,
    handler:  FatalErrorHandler
  )(body: ExecutorServiceTaskExecutor => Unit): Unit = {
    val executor = ExecutorServiceTaskExecutor(settings.copy(shutdownTimeout = 5.seconds), handler)
    try {
      body(executor)
    } finally {
      executor.close()
    }
  }
}
