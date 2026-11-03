package dev.coilix.context

import dev.coilix.config.ComponentProvider
import dev.coilix.context.wiring.AbstractBean
import dev.coilix.context.wiring.DefaultGreetingService
import dev.coilix.context.wiring.EnglishGreeter
import dev.coilix.context.wiring.Farewell
import dev.coilix.context.wiring.Greeter
import dev.coilix.context.wiring.PolishGreeter
import munit.FunSuite

final class ReflectiveBeanDefinitionFactoryTest extends FunSuite {
  private val factory = BeanDefinitionFactory.default(using ComponentProvider.builtIn)

  test("skips abstract classes") {
    assertEquals(factory.create(classOf[AbstractBean]), Nil)
  }

  test("skips interfaces") {
    assertEquals(factory.create(classOf[Greeter]), Nil)
  }

  test("names the bean after its class") {
    assertEquals(factory.create(classOf[EnglishGreeter]).map(_.beanName), List("englishGreeter"))
  }

  test("marks @Primary beans") {
    assertEquals(factory.create(classOf[PolishGreeter]).map(_.isPrimary), List(true))
  }

  test("describes constructor dependencies") {
    assertEquals(
      factory.create(classOf[DefaultGreetingService]).map(_.dependencies),
      List(List(
        Dependency.Required(classOf[Greeter]),
        Dependency.Optional(classOf[Farewell]),
        Dependency.Collection(classOf[Greeter])
      ))
    )
  }
}
