package dev.coilix.context

import dev.coilix.context.collected.CleanupTask
import dev.coilix.context.lifecycle.Journal
import munit.FunSuite

final class ManagedComponentTest extends FunSuite {

  test("is open until closed") {
    val created = CollectedContexts.withTasks().getBean[CollectedComponents].create[CleanupTask](classOf[CleanupTask])
    assertEquals(created.isOpen, true)
  }

  test("runs the destroy methods only once") {
    val context = CollectedContexts.withTasks()
    val created = context.getBean[CollectedComponents].create[CleanupTask](classOf[CleanupTask])
    created.close()
    created.close()
    assertEquals(context.getBean[Journal].all, List("cleanup ready", "cleanup stopped"))
  }

  test("is not destroyed when the context closes") {
    val context = CollectedContexts.withTasks()
    val created = context.getBean[CollectedComponents].create[CleanupTask](classOf[CleanupTask])
    context.close()
    assertEquals((created.isOpen, context.getBean[Journal].all), (true, List("cleanup ready")))
  }
}
