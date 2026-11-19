package dev.coilix.context

import dev.coilix.common.GenericType
import dev.coilix.context.constructor.DependencyShapes
import dev.coilix.context.wiring.Greeter
import munit.FunSuite

final class GenericDependencyTypeMapperTest extends FunSuite {
  private val mapper = GenericDependencyTypeMapper()

  test("maps constructor parameters to dependency kinds") {
    assertEquals(
      classOf[DependencyShapes].getConstructors.toList.flatMap(_.getGenericParameterTypes.toList).map(mapper.map),
      List(
        Dependency.Required(GenericType.of(classOf[Greeter])),
        Dependency.Optional(GenericType.of(classOf[Greeter])),
        Dependency.Collection(GenericType.of(classOf[Greeter])),
        Dependency.Collection(GenericType.of(classOf[Greeter]))
      )
    )
  }

  test("maps a plain class to a required dependency") {
    assertEquals(mapper.map(classOf[String]), Dependency.Required(GenericType.of(classOf[String])))
  }
}
