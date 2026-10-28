package org.scaling.context

import munit.FunSuite
import org.scaling.config.ComponentProvider
import org.scaling.context.wiring.AbstractBean
import org.scaling.context.wiring.DefaultGreetingService
import org.scaling.context.wiring.EnglishGreeter
import org.scaling.context.wiring.Farewell
import org.scaling.context.wiring.Greeter
import org.scaling.context.wiring.PolishGreeter

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
