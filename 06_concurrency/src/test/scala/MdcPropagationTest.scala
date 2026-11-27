package dev.coilix.concurrency

import com.typesafe.config.ConfigFactory
import dev.coilix.config.ComponentProvider
import dev.coilix.config.Environment
import dev.coilix.config.TypesafeConfigTree
import dev.coilix.context.ApplicationContextBuilder
import dev.coilix.context.BeanDefinitionFactory
import dev.coilix.context.ConfigurableApplicationContext
import dev.coilix.context.RegisteredBean
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicReference
import munit.FunSuite
import org.slf4j.MDC
import scala.concurrent.Await
import scala.concurrent.duration.DurationInt

final class MdcPropagationTest extends FunSuite {
  private given ComponentProvider = ComponentProvider.builtIn

  private val mdc = List(MdcContextPropagator())

  override def afterEach(context: AfterEach): Unit = MDC.clear()

  test("a task started with execute sees the MDC of the submitting thread") {
    withCompute { executor =>
      val seen = AtomicReference[String]()
      val done = CountDownLatch(1)
      MDC.put("requestId", "R-1")
      executor.execute { seen.set(MDC.get("requestId")); done.countDown() }
      assert(done.await(5, TimeUnit.SECONDS))
      assertEquals(seen.get, "R-1")
    }
  }

  test("submit and Future combinators on the execution context keep the MDC") {
    withCompute { executor =>
      MDC.put("requestId", "R-2")
      val chained =
        executor.submit(MDC.get("requestId")).map(first => s"$first/${MDC.get("requestId")}")(using executor.executionContext)
      assertEquals(Await.result(chained, 5.seconds), "R-2/R-2")
    }
  }

  test("a pooled thread does not keep the MDC of an earlier task") {
    withCompute { executor =>
      MDC.put("requestId", "R-3")
      val first  = Await.result(executor.submit(MDC.get("requestId")), 5.seconds)
      MDC.clear()
      val second = Await.result(executor.submit(Option(MDC.get("requestId"))), 5.seconds)
      assertEquals((first, second), ("R-3", None))
    }
  }

  test("the blocking executor carries the MDC to its virtual threads") {
    val executor = ExecutorServiceBlockingExecutor(BlockingSettings(shutdownTimeout = 5.seconds), propagators = mdc)
    try {
      MDC.put("requestId", "R-4")
      assertEquals(Await.result(executor.submit(MDC.get("requestId")), 5.seconds), "R-4")
    } finally {
      executor.close()
    }
  }

  test("the context restores every propagator, the first one outermost") {
    val order    = AtomicReference(List.empty[String])
    val snapshot = ContextPropagator.captureAll(List(recording("outer", order), recording("inner", order)))
    snapshot.runWith(order.updateAndGet(_ :+ "task"))
    assertEquals(order.get, List("enter outer", "enter inner", "task", "leave inner", "leave outer"))
  }

  test("the module carries the MDC by default and not with coilix.concurrency.propagate-mdc = false") {
    assertEquals((seenInContext(""), seenInContext("coilix.concurrency.propagate-mdc = false")), (Some("R-6"), None))
  }

  private def seenInContext(config: String): Option[String] = {
    val context = contextOf(config)
    try {
      MDC.put("requestId", "R-6")
      Await.result(context.getBean[TaskExecutor].submit(Option(MDC.get("requestId"))), 5.seconds)
    } finally {
      context.close()
    }
  }

  private def contextOf(config: String): ConfigurableApplicationContext = {
    val environment = Environment.of(TypesafeConfigTree(ConfigFactory.parseString(config)))
    ApplicationContextBuilder
      .default(using ComponentProvider.from(environment))
      .build(
        ConcurrencyModule().components.flatMap(BeanDefinitionFactory.default.create),
        List(RegisteredBean.of[Environment]("environment", environment))
      )
  }

  private def recording(name: String, order: AtomicReference[List[String]]): ContextPropagator =
    () =>
      new ContextSnapshot {
        override def runWith[T](task: => T): T = {
          val _ = order.updateAndGet(_ :+ s"enter $name")
          try {
            task
          } finally {
            val _ = order.updateAndGet(_ :+ s"leave $name")
          }
        }
      }

  private def withCompute(body: ExecutorServiceTaskExecutor => Unit): Unit = {
    val executor = ExecutorServiceTaskExecutor(ComputeSettings(threads = 1, shutdownTimeout = 5.seconds), propagators = mdc)
    try {
      body(executor)
    } finally {
      executor.close()
    }
  }
}
