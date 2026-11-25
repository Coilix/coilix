package dev.coilix.testkit
package effects

import cats.effect.IO
import dev.coilix.concurrency.CoilixIORuntime
import dev.coilix.concurrency.IORuntimeStoppedException
import dev.coilix.concurrency.IORuntimeTaskExecutor
import dev.coilix.testkit.effects.fixture.RecordingFatalErrorHandler
import java.util.concurrent.TimeUnit
import munit.FunSuite

final class CoilixIORuntimeTest extends FunSuite {

  test("reports a shutdown after a fatal error in a fiber to the fatal error handler") {
    val handler = RecordingFatalErrorHandler()
    val owner   = CoilixIORuntime(2, handler)
    IO(throw OutOfMemoryError("simulated in a fiber")).unsafeRunAndForget()(using owner.runtime)
    assert(handler.called.await(5, TimeUnit.SECONDS))
    assertEquals((handler.failures.map(_.getClass), owner.isShutDown), (List(classOf[IORuntimeStoppedException]), true))
  }

  test("does not report a shutdown that the context asked for") {
    val handler = RecordingFatalErrorHandler()
    val owner   = CoilixIORuntime(2, handler)
    owner.close()
    owner.close()
    assertEquals((handler.failures, owner.isShutDown), (Nil, true))
  }

  test("a fatal error of a task goes to the handler and the runtime keeps running") {
    val handler  = RecordingFatalErrorHandler()
    val owner    = CoilixIORuntime(2, handler)
    val executor = IORuntimeTaskExecutor(owner.runtime, handler, Nil)
    try {
      executor.execute(throw StackOverflowError("simulated in a task"))
      assert(handler.called.await(5, TimeUnit.SECONDS))
      assertEquals(
        (handler.failures.map(_.getMessage), IO(42).unsafeRunSync()(using owner.runtime)),
        (List("simulated in a task"), 42)
      )
    } finally {
      owner.close()
    }
  }
}
