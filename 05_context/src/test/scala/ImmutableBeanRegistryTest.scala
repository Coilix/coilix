package dev.coilix.context

import dev.coilix.context.wiring.EnglishGreeter
import dev.coilix.context.wiring.Greeter
import dev.coilix.context.wiring.PolishGreeter
import munit.FunSuite

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
