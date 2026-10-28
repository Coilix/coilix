package org.scaling.context

import munit.FunSuite
import org.scaling.context.constructor.AutowiredConstructorBean
import org.scaling.context.constructor.HiddenConstructorBean
import org.scaling.context.constructor.WidestConstructorBean

final class AutowiredConstructorSelectorTest extends FunSuite {
  private val selector = AutowiredConstructorSelector()

  test("prefers the constructor annotated with @Autowired") {
    assertEquals(selector.select(classOf[AutowiredConstructorBean]).getParameterCount, 1)
  }

  test("falls back to the constructor with the most parameters") {
    assertEquals(selector.select(classOf[WidestConstructorBean]).getParameterCount, 2)
  }

  test("fails when there is no public constructor") {
    intercept[NoPublicConstructorException](selector.select(classOf[HiddenConstructorBean]))
  }
}
