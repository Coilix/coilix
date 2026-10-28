package org.scaling.context

import munit.FunSuite
import org.scaling.config.ValueExpression
import org.scaling.context.configured.ValueBean
import org.scaling.context.wiring.DefaultGreetingService
import org.scaling.context.wiring.Greeter

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
