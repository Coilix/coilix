package dev.coilix.context

import dev.coilix.annotation.Component
import dev.coilix.context.collected.CleanupTask
import dev.coilix.context.collected.ReportTask
import dev.coilix.context.collected.Task
import dev.coilix.context.lifecycle.Journal
import java.beans.JavaBean
import munit.FunSuite

final class DefaultCollectedComponentsTest extends FunSuite {
  private val context    = CollectedContexts.withTasks("tasks.report.enabled = true")
  private val components = context.getBean[CollectedComponents]

  test("lists the collected classes marked with an annotation") {
    assertEquals(
      (components.annotatedWith[JavaBean], components.annotatedWith[Component]),
      (List(classOf[CleanupTask], classOf[ReportTask]), Nil)
    )
  }

  test("creates an initialized instance on each call") {
    val first  = components.create[Task](classOf[CleanupTask])
    val second = components.create[Task](classOf[CleanupTask])
    assert(first.instance ne second.instance)
    assertEquals(context.getBean[Journal].all.count(_ == "cleanup ready"), 2)
  }

  test("runs the destroy methods when the instance is closed") {
    val fresh   = CollectedContexts.withTasks()
    val created = fresh.getBean[CollectedComponents].create[CleanupTask](classOf[CleanupTask])
    created.close()
    assertEquals((created.isOpen, fresh.getBean[Journal].all.lastOption), (false, Some("cleanup stopped")))
  }

  test("rejects a class that is not collected") {
    val error = intercept[BeanCreationException](components.create[Journal](classOf[Journal]))
    assertEquals(error.getMessage, s"Class ${classOf[Journal].getName} is not a collected component")
  }

  test("rejects a requested type the collected class does not have") {
    val error = intercept[BeanCreationException](components.create[Journal](classOf[ReportTask]))
    assertEquals(error.getMessage, s"Collected component ${classOf[ReportTask].getName} is not a ${classOf[Journal].getName}")
  }
}
