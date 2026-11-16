package dev.coilix.context

import dev.coilix.config.Environment
import munit.FunSuite

final class BeanConditionTest extends FunSuite {

  private def environment(hocon: String): Environment =
    Providers.fromHocon(hocon).environment

  test("a profile condition matches an active profile or a negated inactive one") {
    val dev = environment("coilix.profiles.active = [dev, local]")
    assert(ProfileCondition(List("dev")).matches(dev))
    assert(ProfileCondition(List("prod", "local")).matches(dev))
    assert(!ProfileCondition(List("!dev")).matches(dev))
    assert(ProfileCondition(List("!prod")).matches(dev))
    assert(!ProfileCondition(List("prod")).matches(environment("")))
    assertEquals(ProfileCondition(List("dev", "!prod")).description, "one of the profiles dev, !prod matches the active profiles")
  }

  test("a property condition without an expected value accepts anything except false") {
    val condition = PropertyCondition("feature.on")
    assert(condition.matches(environment("feature.on = yes")))
    assert(!condition.matches(environment("feature.on = FALSE")))
    assert(!condition.matches(environment("")))
    assert(PropertyCondition("feature.on", matchIfMissing = true).matches(environment("")))
    assertEquals(condition.description, "configuration 'feature.on' is set and not false")
  }

  test("a property condition with an expected value ignores letter case") {
    val condition = PropertyCondition("mode", "Fast")
    assert(condition.matches(environment("mode = fast")))
    assert(!condition.matches(environment("mode = slow")))
    assertEquals(condition.description, "configuration 'mode' is 'Fast'")
  }

  test("a class condition needs every class") {
    val loader = getClass.getClassLoader
    assert(ClassCondition(List("java.lang.String", "scala.Option"), loader).matches(environment("")))
    assert(!ClassCondition(List("java.lang.String", "com.example.Missing"), loader).matches(environment("")))
    assertEquals(ClassCondition(List("a.B"), loader).description, "classes a.B are on the classpath")
  }
}
