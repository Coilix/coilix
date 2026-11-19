package dev.coilix.context

import dev.coilix.common.GenericType
import dev.coilix.context.effects.Later
import dev.coilix.context.effects.Report
import munit.FunSuite

final class BeanResultTest extends FunSuite {
  private val report = GenericType.of(classOf[Report])
  private val later  = GenericType.Concrete(classOf[Later[?]], List(report))

  test("a plain result keeps the declared type") {
    assertEquals(BeanResult.Plain.beanType(later), later)
  }

  test("an effectful result gives the value type") {
    assertEquals(
      List(BeanResult.Deferred(later, report), BeanResult.Managed(later, report)).map(_.beanType(later)),
      List(report, report)
    )
  }

  test("only effectful results have a declared effect type") {
    assertEquals(
      List(BeanResult.Plain, BeanResult.Deferred(later, report), BeanResult.Managed(later, report)).map(_.declaredType),
      List(None, Some(later), Some(later))
    )
  }

  test("only a plain result is not effectful") {
    assertEquals(
      List(BeanResult.Plain, BeanResult.Deferred(later, report), BeanResult.Managed(later, report)).map(_.isEffectful),
      List(false, true, true)
    )
  }
}
