package dev.coilix.context

import dev.coilix.config.ValueExpression
import dev.coilix.context.configured.ValueBean
import dev.coilix.context.wiring.DefaultGreetingService
import dev.coilix.context.wiring.Greeter
import munit.FunSuite

final class AnnotatedParameterDependencyMapperTest extends FunSuite {
  private val mapper = AnnotatedParameterDependencyMapper(GenericDependencyTypeMapper())

  test("maps a @Value parameter to a value dependency") {
    val parameter = classOf[ValueBean].getConstructors.head.getParameters.head
    assertEquals(mapper.map(parameter), Dependency.Value(ValueExpression.Placeholder("server.host", None), classOf[String]))
  }

  test("maps other parameters by type") {
    val parameter = classOf[DefaultGreetingService].getConstructors.head.getParameters.head
    assertEquals(mapper.map(parameter), Dependency.Required(classOf[Greeter]))
  }
}
