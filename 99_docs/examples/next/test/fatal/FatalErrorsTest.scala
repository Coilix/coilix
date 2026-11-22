package com.example
package fatal

import dev.coilix.concurrency.ProcessTerminator
import dev.coilix.concurrency.TaskExecutor
import dev.coilix.testkit.CoilixContextOps
import dev.coilix.testkit.CoilixTestSetup
import java.util.concurrent.TimeUnit

final class FatalErrorsTest extends munit.FunSuite with CoilixContextOps {
  private val setup = CoilixTestSetup.forApplication[FatalErrorsApplication.type]

  test("a fatal error in a task closes the application with exit code 70") {
    val terminator = RecordingTerminator()
    setup
      .withProperties("coilix.fatal-errors.action" -> "shutdown")
      .replaceBean[ProcessTerminator](terminator)
      .use {
        bean[TaskExecutor].execute(throw StackOverflowError("simulated"))
        assert(terminator.exited.await(5, TimeUnit.SECONDS))
        assertEquals(terminator.codes, List(70))
      }
  }
}
