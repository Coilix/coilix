package dev.coilix.testkit
package effects

import cats.effect.ExitCode
import cats.effect.IO
import cats.effect.unsafe.IORuntime
import dev.coilix.boot.CoilixIOStartup
import dev.coilix.testkit.effects.ioapp.IOAppJournal
import dev.coilix.testkit.effects.ioapp.TillApplication
import munit.FunSuite
import scala.concurrent.duration.DurationInt

final class CoilixIOStartupTest extends FunSuite {
  private given runtime: IORuntime = IORuntime.builder().build()

  private val exitAfterRunners = "--coilix.cats-effect.exit-after-runners=true"

  private val opened = List("drawer opened", "receipts opened")

  private val closed = List("receipts closed", "drawer closed")

  override def beforeEach(context: BeforeEach): Unit = IOAppJournal.clear()

  override def afterAll(): Unit = runtime.shutdown()

  test("registers the given runtime as the ioRuntime bean instead of creating one") {
    val (sameRuntime, names) = CoilixIOStartup
      .application[TillApplication.type](Nil, runtime)
      .use(application => IO((application.context.getBean[IORuntime] eq runtime, application.context.beanNames)))
      .unsafeRunSync()
    assertEquals((sameRuntime, names.contains("coilixIORuntime"), names.contains("ioTaskExecutor")), (true, false, true))
  }

  test("runs IO and plain runners by their order and closes the context after them with exit-after-runners") {
    val exitCode = CoilixIOStartup.run[TillApplication.type](List(exitAfterRunners), runtime).unsafeRunSync()
    assertEquals(
      (exitCode, IOAppJournal.all),
      (ExitCode.Success, opened ++ List("counted 100", "header printed") ++ closed)
    )
  }

  test("runs until it is cancelled, which closes the resources in reverse order") {
    val journal = CoilixIOStartup
      .run[TillApplication.type](Nil, runtime)
      .start
      .flatMap(fiber => waitFor("header printed") *> fiber.cancel)
      .map(_ => IOAppJournal.all)
      .unsafeRunSync()
    assertEquals(journal, opened ++ List("counted 100", "header printed") ++ closed)
  }

  test("a failing runner fails the start after the context closes") {
    val failure = intercept[IllegalStateException](
      CoilixIOStartup.run[TillApplication.type](List("--fail"), runtime).unsafeRunSync()
    )
    assertEquals((failure.getMessage, IOAppJournal.all), ("the till does not add up", opened ++ closed))
  }

  private def waitFor(entry: String): IO[Unit] =
    IO(IOAppJournal.all.contains(entry)).flatMap(found => if (found) IO.unit else IO.sleep(10.millis) *> waitFor(entry))
}
