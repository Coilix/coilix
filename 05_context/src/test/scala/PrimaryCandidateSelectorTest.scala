package org.scaling.context

import munit.FunSuite
import org.scaling.context.wiring.EnglishGreeter
import org.scaling.context.wiring.Farewell
import org.scaling.context.wiring.Greeter
import org.scaling.context.wiring.PolishGreeter

final class PrimaryCandidateSelectorTest extends FunSuite {
  private val selector = PrimaryCandidateSelector()
  private val english  = Definitions.of(classOf[EnglishGreeter])
  private val polish   = Definitions.of(classOf[PolishGreeter])
  private val greeters = english ++ polish

  test("returns nothing when no candidate matches") {
    assertEquals(selector.select(classOf[Farewell], greeters), None)
  }

  test("prefers the exact class") {
    assertEquals(selector.select(classOf[EnglishGreeter], greeters).map(_.beanName), Some("englishGreeter"))
  }

  test("returns the single assignable candidate") {
    assertEquals(selector.select(classOf[Greeter], english).map(_.beanName), Some("englishGreeter"))
  }

  test("prefers the primary candidate among many") {
    assertEquals(selector.select(classOf[Greeter], greeters).map(_.beanName), Some("polishGreeter"))
  }

  test("fails when many candidates match and none is primary") {
    intercept[AmbiguousBeanException](selector.select(classOf[Greeter], english ++ english.map(_.copy(beanName = "other"))))
  }

  test("fails when many candidates are primary") {
    intercept[AmbiguousBeanException](selector.select(classOf[Greeter], polish ++ polish.map(_.copy(beanName = "other"))))
  }
}
