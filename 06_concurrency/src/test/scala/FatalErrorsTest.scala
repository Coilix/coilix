package dev.coilix.concurrency

import dev.coilix.concurrency.fatal.RecordingFatalErrorHandler
import munit.FunSuite

final class FatalErrorsTest extends FunSuite {

  test("treats virtual machine errors and linkage errors as fatal") {
    assertEquals(
      List(OutOfMemoryError(), StackOverflowError(), NoClassDefFoundError(), IllegalStateException(), InterruptedException())
        .map(FatalErrors.isFatal),
      List(true, true, true, false, false)
    )
  }

  test("logs a failure the application survives and passes a fatal error to the handler") {
    val handler = RecordingFatalErrorHandler()
    FatalErrors.guarded("test", handler)(throw IllegalStateException("broken"))
    FatalErrors.guarded("test", handler)(throw OutOfMemoryError("full"))
    assertEquals(
      handler.failures.map((thread, failure) => (thread, failure.getMessage)),
      List((Thread.currentThread.getName, "full"))
    )
  }

  test("rethrows an interruption") {
    val rethrown =
      try {
        FatalErrors.guarded("test", RecordingFatalErrorHandler())(throw InterruptedException("stop"))
        None
      } catch {
        case interrupted: InterruptedException => Some(interrupted.getMessage)
      }
    assertEquals(rethrown, Some("stop"))
  }

  test("the uncaught exception handler passes only fatal errors to the handler") {
    val handler  = RecordingFatalErrorHandler()
    val uncaught = FatalErrors.uncaughtExceptionHandler(handler)
    uncaught.uncaughtException(Thread.currentThread, IllegalStateException("broken"))
    uncaught.uncaughtException(Thread.currentThread, StackOverflowError("deep"))
    assertEquals(handler.failures.map(_._2.getMessage), List("deep"))
  }
}
