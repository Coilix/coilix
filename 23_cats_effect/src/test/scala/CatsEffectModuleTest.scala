package dev.coilix.catseffect

import cats.effect.IO
import cats.effect.unsafe.IORuntime
import cats.syntax.all.*
import dev.coilix.catseffect.CoilixIO.*
import dev.coilix.catseffect.fixture.CurrentTenant
import dev.coilix.concurrency.BlockingExecutor
import dev.coilix.concurrency.ExecutorServiceTaskExecutor
import dev.coilix.concurrency.TaskExecutor
import dev.coilix.config.Environment
import dev.coilix.context.ServiceLoaderModuleLoader
import dev.coilix.testkit.CoilixContextOps
import dev.coilix.testkit.CoilixTestSetup
import munit.FunSuite
import scala.concurrent.Await
import scala.concurrent.duration.DurationInt

final class CatsEffectModuleTest extends FunSuite with CoilixContextOps {
  private val setup =
    CoilixTestSetup.forPackages("dev.coilix.catseffect.fixture").withProperties("coilix.concurrency.compute.threads" -> "2")

  test("is found on the classpath") {
    assert(ServiceLoaderModuleLoader(getClass.getClassLoader, Environment.empty).load().exists(_.name == "cats-effect"))
  }

  test("makes the compute pool of the runtime the only compute pool of the application") {
    setup.use {
      val taskThread = Await.result(bean[TaskExecutor].submit(Thread.currentThread.getName), 5.seconds)
      val ioThreads  = (1 to 200).toList.parTraverse(_ => IO(Thread.currentThread.getName)).unsafeRunSync()(using bean[IORuntime])
      assertEquals(
        (
          bean[TaskExecutor].isInstanceOf[IORuntimeTaskExecutor],
          beanOption[ExecutorServiceTaskExecutor],
          taskThread.startsWith("io-compute-"),
          ioThreads.distinct.size <= 2
        ),
        (true, None, true, true)
      )
    }
  }

  test("tasks keep the context of the thread that submitted them") {
    setup.use {
      val tenant = CurrentTenant.withTenant(Some("acme"))(bean[TaskExecutor].submit(CurrentTenant.get))
      assertEquals(Await.result(tenant, 5.seconds), Some("acme"))
    }
  }

  test("IO.onBlockingExecutor runs on a thread of the blocking executor") {
    setup.use {
      given BlockingExecutor = bean[BlockingExecutor]
      val thread             = IO.onBlockingExecutor(Thread.currentThread.getName).unsafeRunSync()(using bean[IORuntime])
      assert(thread.startsWith("coilix-blocking-"), thread)
    }
  }

  test("closing the context shuts the runtime down") {
    val owner = setup.use(bean[CoilixIORuntime])
    assert(owner.isShutDown)
  }
}
