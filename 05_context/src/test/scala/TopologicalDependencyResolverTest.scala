package dev.coilix.context

import dev.coilix.common.GenericType
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
      resolved(Definitions.of(classOf[DefaultGreetingService], classOf[EnglishGreeter], classOf[PolishGreeter])).map(_.beanName),
      List("englishGreeter", "polishGreeter", "defaultGreetingService")
    )
  }

  test("groups beans that do not depend on each other into one level") {
    assertEquals(
      resolver.resolveLevels(
        Definitions.of(classOf[DefaultGreetingService], classOf[EnglishGreeter], classOf[PolishGreeter]),
        Nil
      ).map(_.map(_.map(_.beanName))),
      Right(List(List("englishGreeter", "polishGreeter"), List("defaultGreetingService")))
    )
  }

  test("ignores optional dependencies without candidates") {
    assertEquals(resolved(Definitions.of(classOf[DefaultGreetingService], classOf[PolishGreeter])).size, 2)
  }

  test("fails when a required dependency is missing") {
    assertEquals(
      resolver.resolve(Definitions.of(classOf[DefaultGreetingService]), Nil),
      Left(ContextError.MissingBean(
        "No bean satisfies dependency 'dev.coilix.context.wiring.Greeter' required by 'defaultGreetingService'"
      ))
    )
  }

  test("accepts dependencies satisfied by provided types") {
    assertEquals(
      resolver.resolve(
        Definitions.of(classOf[ContextAwareBean]),
        List(GenericType.of(classOf[DefaultApplicationContext]))
      ).map(_.size),
      Right(1)
    )
  }

  test("fails on circular dependencies") {
    assertEquals(
      resolver.resolve(Definitions.of(classOf[Chicken], classOf[Egg]), Nil),
      Left(ContextError.CircularDependency(List("chicken", "egg")))
    )
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
    assertEquals(
      resolver.resolve(Definitions.of(classOf[Owl], classOf[Nest]), Nil).left.map(_.getClass.getSimpleName),
      Left("CircularDependency")
    )
  }

  private def names(classes: Class[?]*): List[String] =
    resolved(Definitions.of(classes*)).map(_.beanName)

  private def resolved(definitions: List[BeanDefinition]): List[BeanDefinition] =
    resolver.resolve(definitions, Nil).fold(error => fail(error.message), identity)
}
