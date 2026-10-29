package io.github.coilix.context

import io.github.coilix.config.ValueExpression
import io.github.coilix.context.configured.ValueBean
import io.github.coilix.context.wiring.DefaultGreetingService
import io.github.coilix.context.wiring.Greeter
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
