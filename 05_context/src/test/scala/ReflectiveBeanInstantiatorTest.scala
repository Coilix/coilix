package io.github.coilix.context

import io.github.coilix.config.Environment
import io.github.coilix.context.wiring.DefaultGreetingService
import io.github.coilix.context.wiring.EnglishGreeter
import io.github.coilix.context.wiring.GreetingService
import munit.FunSuite

final class ReflectiveBeanInstantiatorTest extends FunSuite {
  private val instantiator  = ReflectiveBeanInstantiator(Environment.empty)
  private val emptyRegistry = ImmutableBeanRegistry.empty(PrimaryCandidateSelector())
  private val english       = RegisteredBean("englishGreeter", classOf[EnglishGreeter], isPrimary = false, EnglishGreeter())
  private val service       = Definitions.of(classOf[DefaultGreetingService])

  test("creates a bean with its dependencies") {
    assertEquals(
      service.map(instantiator.instantiate(_, emptyRegistry.register(english))).map(bean =>
        classOf[GreetingService].cast(bean.instance).greet("Ann")
      ),
      List("Hello Ann")
    )
  }

  test("fails when a required dependency is not registered") {
    intercept[NoSuchBeanException](service.map(instantiator.instantiate(_, emptyRegistry)))
  }
}
