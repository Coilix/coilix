package org.scaling.context

import munit.FunSuite
import org.scaling.config.ComponentProvider

final class ReflectionsClassPathScannerTest extends FunSuite {
  private val scanner = ClassPathScanner.default(using ComponentProvider.builtIn)

  test("finds concrete annotated classes in a package") {
    assertEquals(
      scanner.scan("org.scaling.context.wiring").map(_.beanName),
      List("contextAwareBean", "defaultGreetingService", "englishGreeter", "polishGreeter")
    )
  }

  test("does not include classes from other packages") {
    assertEquals(scanner.scan("org.scaling.context.cycle").map(_.beanName), List("chicken", "egg"))
  }
}
