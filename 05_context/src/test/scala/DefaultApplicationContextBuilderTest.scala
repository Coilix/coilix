package org.scaling.context

import munit.FunSuite
import org.scaling.config.ComponentProvider
import org.scaling.context.wiring.ContextAwareBean
import org.scaling.context.wiring.DefaultGreetingService
import org.scaling.context.wiring.EnglishGreeter
import org.scaling.context.wiring.GreetingService
import org.scaling.context.wiring.PolishGreeter

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
}
