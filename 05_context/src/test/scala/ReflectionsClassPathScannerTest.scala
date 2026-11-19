package dev.coilix.context

import dev.coilix.config.ComponentProvider
import dev.coilix.context.collected.CleanupTask
import dev.coilix.context.collected.ReportTask
import java.beans.JavaBean
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

  test("finds concrete classes marked with collected annotations without creating definitions") {
    assertEquals(
      scanner.scanAnnotated("dev.coilix.context.collected", List(classOf[JavaBean])),
      List(classOf[CleanupTask], classOf[ReportTask])
    )
  }

  test("finds no annotated classes when no annotation is collected") {
    assertEquals(scanner.scanAnnotated("dev.coilix.context.collected", Nil), Nil)
  }

  test("does not create definitions for classes with collected annotations only") {
    assertEquals(scanner.scan("dev.coilix.context.collected").map(_.beanName), List("taskRegistry"))
  }
}
