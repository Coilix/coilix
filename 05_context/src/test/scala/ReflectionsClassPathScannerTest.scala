package io.github.coilix.context

import io.github.coilix.config.ComponentProvider
import munit.FunSuite

final class ReflectionsClassPathScannerTest extends FunSuite {
  private val scanner = ClassPathScanner.default(using ComponentProvider.builtIn)

  test("finds concrete annotated classes in a package") {
    assertEquals(
      scanner.scan("io.github.coilix.context.wiring").map(_.beanName),
      List("contextAwareBean", "defaultGreetingService", "englishGreeter", "polishGreeter")
    )
  }

  test("does not include classes from other packages") {
    assertEquals(scanner.scan("io.github.coilix.context.cycle").map(_.beanName), List("chicken", "egg"))
  }
}
