package dev.coilix.context

import dev.coilix.config.ComponentProvider
import dev.coilix.context.ordering.AlphaService
import dev.coilix.context.ordering.ItemCatalog
import dev.coilix.context.ordering.YankeeItem
import dev.coilix.context.ordering.ZuluAudit
import dev.coilix.context.ordering.ZuluItem
import dev.coilix.context.wiring.ContextAwareBean
import dev.coilix.context.wiring.DefaultGreetingService
import dev.coilix.context.wiring.EnglishGreeter
import dev.coilix.context.wiring.GreetingService
import dev.coilix.context.wiring.PolishGreeter
import munit.FunSuite

final class DefaultApplicationContextBuilderTest extends FunSuite {
  private given ComponentProvider = ComponentProvider.builtIn

  private val context = ApplicationContextBuilder.default.build(
    Definitions.of(classOf[ContextAwareBean], classOf[DefaultGreetingService], classOf[EnglishGreeter], classOf[PolishGreeter])
  )

  test("injects the primary bean") {
    assertEquals(context.getBean[GreetingService].greet("Ann"), "Cześć Ann")
  }

  test("injects all beans of a collection type") {
    assertEquals(context.getBean[GreetingService].greetAll("Ann").sorted, List("Cześć Ann", "Hello Ann"))
  }

  test("injects none for a missing optional bean") {
    assert(!context.getBean[GreetingService].hasFarewell)
  }

  test("injects the context itself") {
    assert(context.getBean[ContextAwareBean].context eq context)
  }

  test("injected context sees every bean") {
    assertEquals(
      context.getBean[ContextAwareBean].context.beanNames.sorted,
      List("applicationContext", "contextAwareBean", "defaultGreetingService", "englishGreeter", "polishGreeter")
    )
  }

  test("injects a ready singleton") {
    val singleton = RegisteredBean("englishGreeter", classOf[EnglishGreeter], isPrimary = false, EnglishGreeter())
    val built     = ApplicationContextBuilder.default.build(Definitions.of(classOf[DefaultGreetingService]), List(singleton))
    assertEquals(built.getBean[GreetingService].greet("Ann"), "Hello Ann")
  }

  test("builds an active context") {
    assert(context.isActive)
  }

  test("injects optional and collection dependencies defined after their dependant") {
    val built = ApplicationContextBuilder.default.build(
      Definitions.of(classOf[AlphaService], classOf[ItemCatalog], classOf[YankeeItem], classOf[ZuluAudit], classOf[ZuluItem])
    )
    assertEquals((built.getBean[AlphaService].audit.isDefined, built.getBean[ItemCatalog].items.size), (true, 2))
  }
}
