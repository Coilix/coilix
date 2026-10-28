package org.scaling.context

import munit.FunSuite
import org.scaling.context.wiring.EnglishGreeter
import org.scaling.context.wiring.Greeter
import org.scaling.context.wiring.PolishGreeter

final class ImmutableBeanRegistryTest extends FunSuite {
  private val english  = RegisteredBean("englishGreeter", classOf[EnglishGreeter], isPrimary = false, EnglishGreeter())
  private val polish   = RegisteredBean("polishGreeter", classOf[PolishGreeter], isPrimary = true, PolishGreeter())
  private val empty    = ImmutableBeanRegistry.empty(PrimaryCandidateSelector())
  private val registry = empty.register(english).register(polish)

  test("keeps beans in registration order") {
    assertEquals(registry.beans, List(english, polish))
  }

  test("finds a bean by type") {
    assertEquals(registry.find(classOf[Greeter]), Some(polish))
  }

  test("finds all beans of a type") {
    assertEquals(registry.findAll(classOf[Greeter]), List(english, polish))
  }

  test("finds a bean by name") {
    assertEquals(registry.findByName("englishGreeter"), Some(english))
  }

  test("registering leaves the original registry unchanged") {
    assertEquals(empty.beans, Nil)
  }
}
