package dev.coilix.concurrency

import dev.coilix.concurrency.fatal.ClosingResource
import dev.coilix.concurrency.fatal.RecordingTerminator
import dev.coilix.concurrency.fatal.SlowClosingResource
import dev.coilix.config.ComponentProvider
import dev.coilix.context.ApplicationContextBuilder
import dev.coilix.context.BeanDefinitionFactory
import dev.coilix.context.ConfigurableApplicationContext
import java.util.concurrent.TimeUnit
import munit.FunSuite
import scala.concurrent.duration.DurationInt

final class ShutdownFatalErrorHandlerTest extends FunSuite {
  private given ComponentProvider = ComponentProvider.builtIn

  test("closes the context and exits with the configured code") {
    val context    = contextOf(classOf[ClosingResource])
    val terminator = RecordingTerminator()
    val handler    = ShutdownFatalErrorHandler(FatalErrorSettings(exitCode = 71), context, terminator)
    handler.handle(Thread.currentThread, OutOfMemoryError("full"))
    assert(terminator.exited.await(5, TimeUnit.SECONDS))
    assertEquals((context.isActive, context.getBean[ClosingResource].isClosed, terminator.recorded), (false, true, List("exit 71")))
  }

  test("only logs fatal errors that follow the first one") {
    val terminator = RecordingTerminator()
    val handler    = ShutdownFatalErrorHandler(FatalErrorSettings(), contextOf(classOf[ClosingResource]), terminator)
    handler.handle(Thread.currentThread, OutOfMemoryError("first"))
    handler.handle(Thread.currentThread, StackOverflowError("second"))
    assert(terminator.exited.await(5, TimeUnit.SECONDS))
    assertEquals((handler.isShuttingDown, terminator.recorded), (true, List("exit 70")))
  }

  test("halts the process when closing takes longer than the shutdown timeout") {
    val terminator = RecordingTerminator()
    val settings   = FatalErrorSettings(shutdownTimeout = 100.millis)
    val handler    = ShutdownFatalErrorHandler(settings, contextOf(classOf[SlowClosingResource]), terminator)
    handler.handle(Thread.currentThread, OutOfMemoryError("full"))
    assert(terminator.halted.await(5, TimeUnit.SECONDS))
    assert(terminator.exited.await(5, TimeUnit.SECONDS))
    assertEquals(terminator.recorded, List("halt 70", "exit 70"))
  }

  private def contextOf(beanClass: Class[?]): ConfigurableApplicationContext =
    ApplicationContextBuilder.default.build(BeanDefinitionFactory.default.create(beanClass))
}
