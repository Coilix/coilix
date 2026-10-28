package org.scaling.common

import munit.FunSuite
import org.scaling.common.ClassTags.*
import scala.reflect.ClassTag

final class ClassTagsTest extends FunSuite {

  test("exposes the typed runtime class of a class tag") {
    assertEquals(summon[ClassTag[String]].runtimeType, classOf[String])
  }

  test("resolves the runtime class from a context bound") {
    assertEquals(runtimeTypeOf[ScalingException], classOf[ScalingException])
  }
}
