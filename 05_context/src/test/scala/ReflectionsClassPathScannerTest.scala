package dev.coilix.context

import dev.coilix.config.ComponentProvider
import munit.FunSuite

final class ReflectionsClassPathScannerTest extends FunSuite {
  private val scanner = ClassPathScanner.default(using ComponentProvider.builtIn)

  test("finds concrete annotated classes in a package") {
    assertEquals(
      scanner.scan("dev.coilix.context.wiring").map(_.beanName),
      List("contextAwareBean", "defaultGreetingService", "englishGreeter", "polishGreeter")
    )
  }

  test("does not include classes from other packages") {
    assertEquals(scanner.scan("dev.coilix.context.cycle").map(_.beanName), List("chicken", "egg"))
  }
}
