package io.github.coilix.context

import io.github.coilix.context.wiring.EnglishGreeter
import io.github.coilix.context.wiring.Farewell
import io.github.coilix.context.wiring.Greeter
import io.github.coilix.context.wiring.PolishGreeter
import munit.FunSuite

final class DefaultApplicationContextTest extends FunSuite {
  private val english = EnglishGreeter()
  private val polish  = PolishGreeter()
  private val context = DefaultApplicationContext(
    ImmutableBeanRegistry.empty(PrimaryCandidateSelector())
      .register(RegisteredBean("englishGreeter", classOf[EnglishGreeter], isPrimary = false, english))
      .register(RegisteredBean("polishGreeter", classOf[PolishGreeter], isPrimary = true, polish))
  )

  test("returns the primary bean of a type") {
    assertEquals(context.getBean[Greeter], polish)
  }

  test("fails when no bean of a type exists") {
    intercept[NoSuchBeanException](context.getBean[Farewell])
  }

  test("returns none for a missing optional bean") {
    assertEquals(context.getBeanOption[Farewell], None)
  }

  test("returns all beans of a type") {
    assertEquals(context.getBeansOfType[Greeter], List(english, polish))
  }

  test("returns a bean by name") {
    assertEquals(context.getBeanByName("englishGreeter"), Some(english))
  }

  test("lists bean names") {
    assertEquals(context.beanNames, List("englishGreeter", "polishGreeter"))
  }

  test("is active until closed") {
    val closable = DefaultApplicationContext(ImmutableBeanRegistry.empty(PrimaryCandidateSelector()))
    assert(closable.isActive)
    closable.close()
    assert(!closable.isActive)
  }

  test("closing twice keeps the context closed") {
    val closable = DefaultApplicationContext(ImmutableBeanRegistry.empty(PrimaryCandidateSelector()))
    closable.close()
    closable.close()
    assert(!closable.isActive)
  }
}
