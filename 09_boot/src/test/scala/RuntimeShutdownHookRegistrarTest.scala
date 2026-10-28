package org.scaling.boot

import munit.FunSuite
import org.scaling.context.DefaultApplicationContext
import org.scaling.context.ImmutableBeanRegistry
import org.scaling.context.PrimaryCandidateSelector

final class RuntimeShutdownHookRegistrarTest extends FunSuite {

  test("registers a hook that closes the context") {
    val context = DefaultApplicationContext(ImmutableBeanRegistry.empty(PrimaryCandidateSelector()))
    val hook    = RuntimeShutdownHookRegistrar(Runtime.getRuntime).register(context)
    hook.run()
    assert(!context.isActive)
    assert(Runtime.getRuntime.removeShutdownHook(hook))
  }
}
