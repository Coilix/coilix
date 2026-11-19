package dev.coilix.context

import dev.coilix.config.ComponentProvider
import dev.coilix.context.collected.CleanupTask
import dev.coilix.context.collected.ReportTask
import dev.coilix.context.collected.Task
import dev.coilix.context.lifecycle.Journal
import munit.FunSuite

final class DefaultComponentCollectorTest extends FunSuite {
  private given ComponentProvider = ComponentProvider.builtIn

  test("defines the collectedComponents bean with the dependencies of the collected classes") {
    val definition = ComponentCollector.default.collect(List(classOf[CleanupTask]))
    assertEquals(
      (definition.beanName, definition.beanClass, definition.dependencies.headOption),
      ("collectedComponents", classOf[CollectedComponents], Some(Dependency.Required(classOf[Journal])))
    )
  }

  test("leaves out collected classes whose conditions do not hold") {
    assertEquals(CollectedContexts.withTasks().getBean[CollectedComponents].classes, List(classOf[CleanupTask]))
  }

  test("keeps collected classes whose conditions hold") {
    assertEquals(
      CollectedContexts.withTasks("tasks.report.enabled = true").getBean[CollectedComponents].classes,
      List(classOf[CleanupTask], classOf[ReportTask])
    )
  }

  test("does not register collected classes as beans") {
    val context = CollectedContexts.withTasks("tasks.report.enabled = true")
    assertEquals((context.getBeanOption[CleanupTask], context.getBeansOfType[Task]), (None, Nil))
  }

  test("resolves the dependencies and values of collected classes from the context") {
    val components = CollectedContexts.withTasks("tasks.cleanup.target = logs").getBean[CollectedComponents]
    assertEquals(components.create[Task](classOf[CleanupTask]).instance.run(), "cleaning logs")
  }

  test("fails to start when a collected class needs a missing bean") {
    val error = intercept[NoSuchBeanException] {
      CollectedContexts.contextOf("", Nil, List(classOf[CleanupTask])).beanNames
    }
    assert(error.getMessage.contains(classOf[Journal].getName))
  }

  test("collects nothing from an empty list") {
    assertEquals(CollectedContexts.contextOf("", Nil, Nil).getBean[CollectedComponents].classes, Nil)
  }
}
