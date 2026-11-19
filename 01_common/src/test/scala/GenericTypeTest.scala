package dev.coilix.common

import dev.coilix.common.GenericType.Concrete
import dev.coilix.common.GenericType.Variable
import dev.coilix.common.GenericType.Wildcard
import dev.coilix.common.types.ArrayHolder
import dev.coilix.common.types.Consumer
import dev.coilix.common.types.Effect
import dev.coilix.common.types.EffectRepo
import dev.coilix.common.types.GenericRepo
import dev.coilix.common.types.Repo
import munit.FunSuite

final class GenericTypeTest extends FunSuite {
  private val parameters = classOf[Consumer].getConstructors.head.getGenericParameterTypes.toList.map(GenericType.of)

  private val effectRepo = Concrete(classOf[Repo[?]], List(Concrete(classOf[Effect[?]], Nil)))
  private val optionRepo = Concrete(classOf[Repo[?]], List(Concrete(classOf[Option[?]], Nil)))

  test("reads type constructor arguments from constructor parameters") {
    assertEquals(parameters.take(2), List(effectRepo, optionRepo))
  }

  test("reads primitive arguments as Object and wildcards as Wildcard") {
    assertEquals(
      parameters.drop(2),
      List(Concrete(classOf[Option[?]], List(Concrete(classOf[Object], Nil))), Concrete(classOf[Option[?]], List(Wildcard)))
    )
  }

  test("matches a class that extends the type with the same arguments") {
    assert(effectRepo.isAssignableFrom(GenericType.of(classOf[EffectRepo])))
  }

  test("does not match a class that extends the type with other arguments") {
    assert(!optionRepo.isAssignableFrom(GenericType.of(classOf[EffectRepo])))
  }

  test("does not match a generic class whose type variable is not bound") {
    assert(!effectRepo.isAssignableFrom(GenericType.of(classOf[GenericRepo[?]])))
  }

  test("matches a generic class once its type variable is bound") {
    assert(effectRepo.isAssignableFrom(Concrete(classOf[GenericRepo[?]], List(Concrete(classOf[Effect[?]], Nil)))))
  }

  test("compares only classes when the expected type has no arguments") {
    assert(GenericType.of(classOf[Repo[?]]).isAssignableFrom(GenericType.of(classOf[GenericRepo[?]])))
  }

  test("matches any argument with a wildcard") {
    assert(Concrete(classOf[Repo[?]], List(Wildcard)).isAssignableFrom(GenericType.of(classOf[EffectRepo])))
  }

  test("sees a type as its supertype with substituted arguments") {
    assertEquals(
      Concrete(classOf[GenericRepo[?]], List(Concrete(classOf[Option[?]], Nil))).asSuper(classOf[Repo[?]]),
      Some(optionRepo)
    )
  }

  test("lists unbound type variables") {
    assertEquals(GenericType.of(classOf[GenericRepo[?]]).asSuper(classOf[Repo[?]]).map(_.variables), Some(List("F")))
  }

  test("returns no supertype for an unrelated class") {
    assertEquals(effectRepo.asSuper(classOf[String]), None)
  }

  test("shows the type with fully qualified names") {
    assertEquals(
      (effectRepo.toString, Variable("F").toString, Wildcard.toString),
      ("dev.coilix.common.types.Repo[dev.coilix.common.types.Effect]", "F", "?")
    )
  }

  test("erases variables and wildcards to Object") {
    assertEquals((Variable("F").erasure, Wildcard.erasure), (classOf[Object], classOf[Object]))
  }

  test("reads a type variable from reflection") {
    assertEquals(GenericType.of(classOf[GenericRepo[?]].getTypeParameters.head), Variable("F"))
  }

  test("reads generic arrays as array classes") {
    val parameter = classOf[ArrayHolder[?]].getConstructors.head.getGenericParameterTypes.head
    assertEquals(GenericType.of(parameter).erasure, classOf[Array[List[?]]])
  }
}
