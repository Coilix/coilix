package dev.coilix.context

import dev.coilix.context.lifecycle.BrokenShutdown
import dev.coilix.context.lifecycle.Connection
import dev.coilix.context.lifecycle.DestroyWithResult
import dev.coilix.context.lifecycle.InitWithArgument
import dev.coilix.context.lifecycle.Journal
import dev.coilix.context.lifecycle.OrderedInit
import dev.coilix.context.lifecycle.Repository
import dev.coilix.context.lifecycle.Worker
import munit.FunSuite

final class AnnotatedLifecycleMethodScannerTest extends FunSuite {
  private val scanner = AnnotatedLifecycleMethodScanner()

  private def initNames(beanClass: Class[?]): List[String] =
    scanner.scan("bean", beanClass).init.map(_.getName)

  private def destroyNames(beanClass: Class[?]): List[String] =
    scanner.scan("bean", beanClass).destroy.map(_.getName)

  test("finds private init methods and public destroy methods") {
    assertEquals((initNames(classOf[Connection]), destroyNames(classOf[Connection])), (List("open"), List("close")))
  }

  test("finds methods declared without parentheses") {
    assertEquals(destroyNames(classOf[Repository]), List("flush"))
  }

  test("orders methods by name") {
    assertEquals(initNames(classOf[OrderedInit]), List("first", "second"))
    assertEquals(destroyNames(classOf[BrokenShutdown]), List("fail", "report"))
  }

  test("finds private methods of superclasses") {
    assertEquals(initNames(classOf[Worker]), List("announce"))
  }

  test("gives no methods to a bean without lifecycle annotations") {
    assertEquals(scanner.scan("journal", classOf[Journal]), LifecycleMethods.none)
  }

  test("rejects an init method with parameters") {
    val error = intercept[InvalidLifecycleMethodException](scanner.scan("initWithArgument", classOf[InitWithArgument]))
    assertEquals(error.getMessage, "@PostConstruct method init of bean 'initWithArgument' must not take parameters")
  }

  test("rejects a destroy method that returns a value") {
    val error = intercept[InvalidLifecycleMethodException](scanner.scan("destroyWithResult", classOf[DestroyWithResult]))
    assertEquals(error.getMessage, "@PreDestroy method shutdown of bean 'destroyWithResult' must return Unit")
  }
}
