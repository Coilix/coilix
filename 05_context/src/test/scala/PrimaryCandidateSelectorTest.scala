package dev.coilix.context

import dev.coilix.common.GenericType
import dev.coilix.context.wiring.EnglishGreeter
import dev.coilix.context.wiring.Farewell
import dev.coilix.context.wiring.Greeter
import dev.coilix.context.wiring.PolishGreeter
import munit.FunSuite

final class PrimaryCandidateSelectorTest extends FunSuite {
  private val selector = PrimaryCandidateSelector()
  private val english  = Definitions.of(classOf[EnglishGreeter])
  private val polish   = Definitions.of(classOf[PolishGreeter])
  private val greeters = english ++ polish

  test("returns nothing when no candidate matches") {
    assertEquals(selector.select(GenericType.of(classOf[Farewell]), greeters), None)
  }

  test("prefers the exact class") {
    assertEquals(selector.select(GenericType.of(classOf[EnglishGreeter]), greeters).map(_.beanName), Some("englishGreeter"))
  }

  test("returns the single assignable candidate") {
    assertEquals(selector.select(GenericType.of(classOf[Greeter]), english).map(_.beanName), Some("englishGreeter"))
  }

  test("prefers the primary candidate among many") {
    assertEquals(selector.select(GenericType.of(classOf[Greeter]), greeters).map(_.beanName), Some("polishGreeter"))
  }

  test("gives an ambiguity error when many candidates match and none is primary") {
    assertEquals(
      selector.choose(GenericType.of(classOf[Greeter]), english ++ english.map(_.copy(beanName = "other"))).left.map(_.candidates),
      Left(List("englishGreeter", "other"))
    )
  }

  test("select throws the ambiguity error") {
    intercept[AmbiguousBeanException](selector.select(
      GenericType.of(classOf[Greeter]),
      english ++ english.map(_.copy(beanName = "other"))
    ))
  }

  test("fails when many candidates are primary") {
    intercept[AmbiguousBeanException](selector.select(
      GenericType.of(classOf[Greeter]),
      polish ++ polish.map(_.copy(beanName = "other"))
    ))
  }
}
