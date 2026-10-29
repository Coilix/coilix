package io.github.coilix.context

import io.github.coilix.config.ComponentProvider
import io.github.coilix.context.wiring.AbstractBean
import io.github.coilix.context.wiring.DefaultGreetingService
import io.github.coilix.context.wiring.EnglishGreeter
import io.github.coilix.context.wiring.Farewell
import io.github.coilix.context.wiring.Greeter
import io.github.coilix.context.wiring.PolishGreeter
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
