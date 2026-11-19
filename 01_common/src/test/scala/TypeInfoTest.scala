package dev.coilix.common

import dev.coilix.common.GenericType.Concrete
import dev.coilix.common.GenericType.Wildcard
import dev.coilix.common.types.Consumer
import dev.coilix.common.types.Effect
import dev.coilix.common.types.EffectRepo
import dev.coilix.common.types.Repo
import munit.FunSuite
import scala.reflect.ClassTag

final class TypeInfoTest extends FunSuite {
  private val reflected = classOf[Consumer].getConstructors.head.getGenericParameterTypes.toList.map(GenericType.of)

  private def viaClassTag[T: ClassTag]: GenericType = TypeInfo[T].genericType

  test("derives a type equal to the one read from reflection") {
    assertEquals(
      List(
        TypeInfo[Repo[Effect]].genericType,
        TypeInfo[Repo[Option]].genericType,
        TypeInfo[Option[Int]].genericType,
        TypeInfo[Option[?]].genericType
      ),
      reflected
    )
  }

  test("derives a class without arguments for a plain type") {
    assertEquals(TypeInfo[EffectRepo].genericType, Concrete(classOf[EffectRepo], Nil))
  }

  test("follows type aliases") {
    type EffectRepository = Repo[Effect]
    assertEquals(TypeInfo[EffectRepository].genericType, reflected.head)
  }

  test("falls back to the class tag for an abstract type") {
    assertEquals(viaClassTag[List[String]], Concrete(classOf[List[?]], Nil))
  }

  test("gives the runtime class and checks instances by class") {
    val info = TypeInfo[Repo[Effect]]
    assertEquals[Any, Any](
      (info.runtimeClass, info.isInstance(EffectRepo()), info.isInstance("text")),
      (classOf[Repo[?]], true, false)
    )
  }

  test("creates type information from a class or a generic type") {
    assertEquals(
      (TypeInfo.fromClass[Any](classOf[String]).genericType, TypeInfo.of[Any](Wildcard).genericType),
      (Concrete(classOf[String], Nil), Wildcard)
    )
  }

  test("rejects a type lambda at compile time") {
    assert(compileErrors("TypeInfo[Repo[[A] =>> Either[String, A]]]").contains("type lambda"))
  }
}
