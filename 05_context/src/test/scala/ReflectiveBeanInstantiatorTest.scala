package org.scaling.context

import munit.FunSuite
import org.scaling.config.Environment
import org.scaling.context.wiring.DefaultGreetingService
import org.scaling.context.wiring.EnglishGreeter
import org.scaling.context.wiring.GreetingService

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
