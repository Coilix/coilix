package org.scaling.config

import munit.FunSuite
import org.scaling.config.fixture.MailSettings
import org.scaling.config.fixture.PrefixGreeting

final class ScalaDefaultArgumentsTest extends FunSuite {
  private val defaults = ScalaDefaultArguments()

  test("finds a default constructor argument") {
    assertEquals(defaults.find(classOf[MailSettings], 4), Some(Boolean.box(false)))
  }

  test("returns none for a parameter without a default") {
    assertEquals(defaults.find(classOf[MailSettings], 0), None)
  }

  test("returns none for a class without a companion") {
    assertEquals(defaults.find(classOf[PrefixGreeting], 0), None)
  }
}
