package dev.coilix.context

import dev.coilix.common.GenericType
import dev.coilix.config.ComponentProvider
import dev.coilix.context.tagless.Async
import dev.coilix.context.tagless.Effect
import dev.coilix.context.tagless.EffectConfiguration
import dev.coilix.context.tagless.InMemoryOrders
import dev.coilix.context.tagless.OrderRepository
import dev.coilix.context.tagless.OrderService
import dev.coilix.context.tagless.Shelf
import dev.coilix.context.tagless.Sync
import dev.coilix.context.tagless.TwoEffects
import munit.FunSuite

final class EffectTypeBindingTest extends FunSuite {
  private given ComponentProvider = ComponentProvider.builtIn

  private val binding = EffectTypeBinding(classOf[Effect[?]])

  private def definitionsOf(classes: Class[?]*): List[BeanDefinition] = classes.toList.flatMap(BeanDefinitionFactory.default.create)

  private def typed(raw: Class[?], argument: Class[?]): GenericType =
    GenericType.Concrete(raw, List(GenericType.Concrete(argument, Nil)))

  test("a component keeps its type parameter unbound when it appears in the types of its dependencies") {
    val service = definitionsOf(classOf[OrderService[?]]).head
    assertEquals(service.beanType, GenericType.Concrete(classOf[OrderService[?]], List(GenericType.Variable("F"))))
    assertEquals(EffectTypeBinding.variablesOf(service), List("F"))
  }

  test("binds the type parameter in the type of the bean and of every dependency") {
    val service = binding.bind(definitionsOf(classOf[OrderService[?]]).head)
    assertEquals(service.beanType, typed(classOf[OrderService[?]], classOf[Effect[?]]))
    assertEquals(
      service.dependencies,
      List(
        Dependency.Required(typed(classOf[OrderRepository[?]], classOf[Effect[?]])),
        Dependency.Required(typed(classOf[Async[?]], classOf[Effect[?]]))
      )
    )
  }

  test("creates tagless final components and generic @Bean methods with the type class instance of the effect") {
    val context = ApplicationContextBuilder.default.build(
      definitionsOf(classOf[OrderService[?]], classOf[EffectConfiguration]).map(binding.bind)
    )
    val service = context.getBean[OrderService[Effect]]
    val async   = context.getBean[Async[Effect]]
    assertEquals((service.async eq async, service.repo.isInstanceOf[InMemoryOrders[?]]), (true, true))
    assertEquals(service.repo, context.getBean[OrderRepository[Effect]])
    assert(context.getBean[OrderRepository[Effect]] match {
      case orders: InMemoryOrders[?] => orders.sync eq async
      case _ => false
    })
    assertEquals(context.getBean[Sync[Effect]], async)
  }

  test("leaves components without type parameters in their dependencies unchanged") {
    val shelf = definitionsOf(classOf[Shelf[?]]).head
    assertEquals((binding.bind(shelf), shelf.beanType), (shelf, GenericType.of(classOf[Shelf[?]])))
  }

  test("rejects a component with two type parameters to bind") {
    val error = intercept[UnsupportedDependencyTypeException](binding.bind(definitionsOf(classOf[TwoEffects[?, ?]]).head))
    assert(error.getMessage.contains("leaves the type parameters F, G unbound"), error.getMessage)
  }

  test("a context with an unbound type parameter fails to plan with a hint about the effect module") {
    val error = intercept[UnsupportedDependencyTypeException](
      ApplicationContextBuilder.default.build(definitionsOf(classOf[OrderService[?]], classOf[EffectConfiguration]))
    )
    assert(error.getMessage.contains("coilix-cats-effect"), error.getMessage)
  }
}
