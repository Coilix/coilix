package org.scaling.context

import munit.FunSuite
import org.scaling.context.cycle.Chicken
import org.scaling.context.cycle.Egg
import org.scaling.context.wiring.ContextAwareBean
import org.scaling.context.wiring.DefaultGreetingService
import org.scaling.context.wiring.EnglishGreeter
import org.scaling.context.wiring.PolishGreeter

final class TopologicalDependencyResolverTest extends FunSuite {
  private val resolver = TopologicalDependencyResolver(PrimaryCandidateSelector())

  test("orders dependencies before their dependants") {
    assertEquals(
      resolver.resolve(
        Definitions.of(classOf[DefaultGreetingService], classOf[EnglishGreeter], classOf[PolishGreeter]),
        Nil
      ).map(_.beanName),
      List("englishGreeter", "polishGreeter", "defaultGreetingService")
    )
  }

  test("ignores optional dependencies without candidates") {
    assertEquals(resolver.resolve(Definitions.of(classOf[DefaultGreetingService], classOf[PolishGreeter]), Nil).size, 2)
  }

  test("fails when a required dependency is missing") {
    intercept[NoSuchBeanException](resolver.resolve(Definitions.of(classOf[DefaultGreetingService]), Nil))
  }

  test("accepts dependencies satisfied by provided types") {
    assertEquals(resolver.resolve(Definitions.of(classOf[ContextAwareBean]), List(classOf[DefaultApplicationContext])).size, 1)
  }

  test("fails on circular dependencies") {
    intercept[CircularDependencyException](resolver.resolve(Definitions.of(classOf[Chicken], classOf[Egg]), Nil))
  }
}
