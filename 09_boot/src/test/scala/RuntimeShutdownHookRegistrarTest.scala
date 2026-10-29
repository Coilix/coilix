package io.github.coilix.boot

import io.github.coilix.context.DefaultApplicationContext
import io.github.coilix.context.ImmutableBeanRegistry
import io.github.coilix.context.PrimaryCandidateSelector
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
