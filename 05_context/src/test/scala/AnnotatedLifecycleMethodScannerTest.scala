package dev.coilix.context

import dev.coilix.context.effects.LaterCount
import dev.coilix.context.effects.LaterWarmup
import dev.coilix.context.effects.LaterWarmups
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
    assertEquals(
      error.getMessage,
      "@PreDestroy method shutdown of bean 'destroyWithResult' must return Unit or an effect of Unit, such as IO[Unit]"
    )
  }

  test("accepts methods that return an effect of Unit recognized as deferred") {
    val methods = LaterWarmups.scanner.scan("laterWarmup", classOf[LaterWarmup])
    assertEquals(
      (methods.plainInit.map(_.getName), methods.effectfulInit.map(_.getName)),
      (List("prepare"), List("warm"))
    )
    assertEquals(
      (methods.plainDestroy.map(_.getName), methods.effectfulDestroy.map(_.getName), methods.hasEffects),
      (List("stop"), List("cool"), true)
    )
  }

  test("rejects an effect that is not recognized") {
    val error = intercept[InvalidLifecycleMethodException](scanner.scan("laterWarmup", classOf[LaterWarmup]))
    assert(error.getMessage.contains("must return Unit or an effect of Unit"), error.getMessage)
  }

  test("rejects an effect of a value other than Unit") {
    val error = intercept[InvalidLifecycleMethodException](LaterWarmups.scanner.scan("laterCount", classOf[LaterCount]))
    assert(error.getMessage.startsWith("@PostConstruct method count of bean 'laterCount' must return Unit or"), error.getMessage)
  }
}
