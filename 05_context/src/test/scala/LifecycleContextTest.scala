package dev.coilix.context

import dev.coilix.config.ComponentProvider
import dev.coilix.context.lifecycle.BrokenShutdown
import dev.coilix.context.lifecycle.Connection
import dev.coilix.context.lifecycle.FailingInit
import dev.coilix.context.lifecycle.FailingStart
import dev.coilix.context.lifecycle.InitWithArgument
import dev.coilix.context.lifecycle.Journal
import dev.coilix.context.lifecycle.OrderedInit
import dev.coilix.context.lifecycle.Repository
import dev.coilix.context.lifecycle.StartWatcher
import dev.coilix.context.lifecycle.Worker
import munit.FunSuite

final class LifecycleContextTest extends FunSuite {
  private given ComponentProvider = ComponentProvider.builtIn

  private def contextOf(classes: Class[?]*): ConfigurableApplicationContext =
    ApplicationContextBuilder.default.build(Definitions.of((classOf[Journal] +: classes)*))

  test("initializes a bean before it is injected into another bean") {
    val context = contextOf(classOf[Repository], classOf[Connection])
    assertEquals(context.getBean[Journal].all, List("connection opened", "repository created, connection open: true"))
  }

  test("runs init methods of one bean by name and init methods of superclasses") {
    val context = contextOf(classOf[OrderedInit], classOf[Worker])
    assertEquals(context.getBean[Journal].all.sorted, List("Worker started", "first", "second"))
    assertEquals(context.getBean[Journal].all.filter(_ != "Worker started"), List("first", "second"))
  }

  test("destroys beans in reverse creation order and continues after a failure") {
    val context = contextOf(classOf[BrokenShutdown], classOf[Repository], classOf[Connection])
    context.close()
    assertEquals(
      context.getBean[Journal].all.drop(2),
      List("broken shutdown reported", "repository flushed", "connection closed")
    )
  }

  test("destroys beans only once") {
    val context = contextOf(classOf[Connection])
    context.close()
    context.close()
    assertEquals(context.getBean[Journal].all.count(_ == "connection closed"), 1)
  }

  test("wraps an exception from an init method with the bean and method name") {
    val error = intercept[BeanCreationException](contextOf(classOf[FailingInit]).beanNames)
    assertEquals(
      (error.getMessage, error.getCause.getMessage),
      ("@PostConstruct method init of bean 'failingInit' failed: java.lang.IllegalStateException: no connection", "no connection")
    )
  }

  test("rejects a wrong lifecycle signature while creating definitions") {
    intercept[InvalidLifecycleMethodException](Definitions.of(classOf[InitWithArgument]))
  }

  test("notifies listeners after every bean is created and before beans are destroyed") {
    val context = contextOf(classOf[StartWatcher], classOf[Connection])
    context.close()
    assertEquals(
      context.getBean[Journal].all.filterNot(_.startsWith("connection")),
      List("started with applicationContext, connection, journal, startWatcher", "closing", "watcher destroyed")
    )
  }

  test("a failing listener closes the context and stops the startup") {
    val journal = Journal()
    val failure = intercept[IllegalStateException](
      ApplicationContextBuilder.default.build(
        Definitions.of(classOf[FailingStart], classOf[Connection]),
        List(RegisteredBean.of("journal", journal))
      )
    )
    assertEquals((failure.getMessage, journal.all.last), ("cannot start", "connection closed"))
  }
}
