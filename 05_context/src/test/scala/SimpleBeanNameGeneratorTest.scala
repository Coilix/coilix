package org.scaling.context

import munit.FunSuite
import org.scaling.context.wiring.EnglishGreeter
import org.scaling.context.wiring.Greeter

final class SimpleBeanNameGeneratorTest extends FunSuite {
  private val generator = SimpleBeanNameGenerator()

  test("decapitalizes the simple class name") {
    assertEquals(generator.generate(classOf[EnglishGreeter]), "englishGreeter")
  }

  test("falls back to the full name for anonymous classes") {
    val anonymous = new Greeter { override def greet(name: String): String = name }
    assertEquals(generator.generate(anonymous.getClass), anonymous.getClass.getName)
  }
}
