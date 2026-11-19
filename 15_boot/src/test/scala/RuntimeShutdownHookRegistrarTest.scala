package dev.coilix.boot

import dev.coilix.context.DefaultApplicationContext
import dev.coilix.context.ImmutableBeanRegistry
import dev.coilix.context.PrimaryCandidateSelector
import munit.FunSuite

final class RuntimeShutdownHookRegistrarTest extends FunSuite {

  test("registers a hook that closes the context") {
    val context = DefaultApplicationContext(ImmutableBeanRegistry.empty(PrimaryCandidateSelector()))
    val hook    = RuntimeShutdownHookRegistrar(Runtime.getRuntime).register(context)
    hook.run()
    assert(!context.isActive)
    assert(Runtime.getRuntime.removeShutdownHook(hook))
  }
}
