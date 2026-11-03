package dev.coilix.context

import dev.coilix.context.constructor.DependencyShapes
import dev.coilix.context.wiring.Greeter
import munit.FunSuite

final class GenericDependencyTypeMapperTest extends FunSuite {
  private val mapper = GenericDependencyTypeMapper()

  test("maps constructor parameters to dependency kinds") {
    assertEquals(
      classOf[DependencyShapes].getConstructors.toList.flatMap(_.getGenericParameterTypes.toList).map(mapper.map),
      List(
        Dependency.Required(classOf[Greeter]),
        Dependency.Optional(classOf[Greeter]),
        Dependency.Collection(classOf[Greeter]),
        Dependency.Collection(classOf[Greeter])
      )
    )
  }

  test("maps a plain class to a required dependency") {
    assertEquals(mapper.map(classOf[String]), Dependency.Required(classOf[String]))
  }
}
