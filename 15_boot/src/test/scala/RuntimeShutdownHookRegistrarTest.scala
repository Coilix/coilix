package dev.coilix.boot

import dev.coilix.concurrency.FatalErrorHandler
import dev.coilix.context.DefaultApplicationContext
import dev.coilix.context.ImmutableBeanRegistry
import dev.coilix.context.PrimaryCandidateSelector
import dev.coilix.context.RegisteredBean
import java.util.concurrent.atomic.AtomicReference
import munit.FunSuite

final class RuntimeShutdownHookRegistrarTest extends FunSuite {

  test("registers a hook that closes the context") {
    val context = DefaultApplicationContext(ImmutableBeanRegistry.empty(PrimaryCandidateSelector()))
    val hook    = RuntimeShutdownHookRegistrar(Runtime.getRuntime).register(context)
    hook.run()
    assert(!context.isActive)
    assert(Runtime.getRuntime.removeShutdownHook(hook))
  }

  test("passes fatal errors of every thread to the handler of the context") {
    val handled  = AtomicReference(List.empty[String])
    val handler  = new FatalErrorHandler {
      override def handle(thread: Thread, failure: Throwable): Unit = {
        val _ = handled.updateAndGet(_ :+ failure.getMessage)
      }
    }
    val registry = ImmutableBeanRegistry
      .empty(PrimaryCandidateSelector())
      .register(RegisteredBean.of[FatalErrorHandler]("fatalErrorHandler", handler))
    val previous = Thread.getDefaultUncaughtExceptionHandler
    val hook     = RuntimeShutdownHookRegistrar(Runtime.getRuntime).register(DefaultApplicationContext(registry))
    try {
      Thread.getDefaultUncaughtExceptionHandler.uncaughtException(Thread.currentThread, OutOfMemoryError("full"))
      assertEquals(handled.get, List("full"))
    } finally {
      Thread.setDefaultUncaughtExceptionHandler(previous)
      val _ = Runtime.getRuntime.removeShutdownHook(hook)
    }
  }
}
