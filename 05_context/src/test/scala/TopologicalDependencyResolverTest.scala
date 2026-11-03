package dev.coilix.context

import dev.coilix.context.cycle.Chicken
import dev.coilix.context.cycle.Egg
import dev.coilix.context.ordering.AlphaService
import dev.coilix.context.ordering.CompositeItem
import dev.coilix.context.ordering.ItemCatalog
import dev.coilix.context.ordering.Nest
import dev.coilix.context.ordering.Owl
import dev.coilix.context.ordering.YankeeItem
import dev.coilix.context.ordering.ZuluAudit
import dev.coilix.context.ordering.ZuluItem
import dev.coilix.context.wiring.ContextAwareBean
import dev.coilix.context.wiring.DefaultGreetingService
import dev.coilix.context.wiring.EnglishGreeter
import dev.coilix.context.wiring.PolishGreeter
import munit.FunSuite

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

  test("orders an optional dependency before its dependant") {
    assertEquals(names(classOf[AlphaService], classOf[ZuluAudit]), List("zuluAudit", "alphaService"))
  }

  test("orders every collection candidate before the collection") {
    assertEquals(names(classOf[ItemCatalog], classOf[ZuluItem], classOf[YankeeItem]), List("zuluItem", "yankeeItem", "itemCatalog"))
  }

  test("does not wait for itself in a collection of its own type") {
    assertEquals(names(classOf[CompositeItem], classOf[ZuluItem]), List("zuluItem", "compositeItem"))
  }

  test("fails on a cycle through an optional dependency") {
    intercept[CircularDependencyException](resolver.resolve(Definitions.of(classOf[Owl], classOf[Nest]), Nil))
  }

  private def names(classes: Class[?]*): List[String] =
    resolver.resolve(Definitions.of(classes*), Nil).map(_.beanName)
}
