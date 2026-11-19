package dev.coilix.boot

import dev.coilix.boot.fixture.Clock
import dev.coilix.boot.fixture.FixedClock
import dev.coilix.boot.modules.ReportTitle
import dev.coilix.common.GenericType
import dev.coilix.config.ComponentProvider
import dev.coilix.context.BeanDefinition
import dev.coilix.context.BeanDefinitionFactory
import munit.FunSuite

final class BeanReplacementsTest extends FunSuite {
  private given ComponentProvider = ComponentProvider.builtIn

  private val fixedClock: List[BeanDefinition] = BeanDefinitionFactory.default.create(classOf[FixedClock])

  test("replaces a definition whose class implements the replaced type") {
    assert(fixedClock.forall(BeanReplacements.of[Clock].replaces))
  }

  test("does not replace a definition of an unrelated type") {
    assert(!fixedClock.exists(BeanReplacements.of[ReportTitle].replaces))
  }

  test("none replaces nothing") {
    assert(!fixedClock.exists(BeanReplacements.none.replaces))
  }

  test("lists the types that match no definition") {
    assertEquals(BeanReplacements.of[Clock].and[ReportTitle].unmatched(fixedClock), List(GenericType.of(classOf[ReportTitle])))
  }
}
