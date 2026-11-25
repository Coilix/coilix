package dev.coilix.concurrency

import dev.coilix.common.GenericType
import dev.coilix.concurrency.async.PdfReportRenderer
import dev.coilix.concurrency.async.ReportDesk
import dev.coilix.concurrency.async.ReportRenderer
import dev.coilix.concurrency.invalid.AsyncWithResult
import dev.coilix.concurrency.invalid.AsyncWithoutTrait
import dev.coilix.config.ComponentProvider
import dev.coilix.context.ApplicationContextBuilder
import dev.coilix.context.BeanDefinitionFactory
import dev.coilix.context.ConfigurableApplicationContext
import dev.coilix.context.Dependency
import dev.coilix.context.ProxiedBean
import java.util.concurrent.TimeUnit
import munit.FunSuite
import scala.concurrent.Await
import scala.concurrent.duration.DurationInt

final class AsyncDefinitionProcessorTest extends FunSuite {
  private given ComponentProvider = ComponentProvider.builtIn

  private val processor = AsyncDefinitionProcessor()

  test("registers a bean with @Async methods under its trait and depends on both executors") {
    val definition = processor.process(BeanDefinitionFactory.default.create(classOf[PdfReportRenderer]).head)
    assertEquals(
      (definition.beanType, definition.dependencies.takeRight(2)),
      (
        GenericType.of(classOf[ReportRenderer]),
        List(
          Dependency.Required(GenericType.of(classOf[TaskExecutor])),
          Dependency.Required(GenericType.of(classOf[BlockingExecutor]))
        )
      )
    )
  }

  test("runs a blocking @Async method on the blocking executor") {
    withContext { context =>
      val result = Await.result(context.getBean[ReportRenderer].archive("sales"), 5.seconds)
      assert(result.startsWith("sales archived on coilix-blocking-"), result)
    }
  }

  test("leaves other definitions as they are") {
    val definition = BeanDefinitionFactory.default.create(classOf[ReportDesk]).head
    assertEquals(processor.process(definition), definition)
  }

  test("runs a method returning a Future on the executor") {
    withContext { context =>
      val result = Await.result(context.getBean[ReportDesk].renderer.render("sales"), 5.seconds)
      assert(result.startsWith("sales on io-compute-"), result)
    }
  }

  test("starts a Unit method on the executor without waiting") {
    withContext { context =>
      context.getBean[ReportRenderer].announce("sales")
      val renderer  = target(context)
      assertEquals(renderer.events.poll(), "started")
      val announced = renderer.events.poll(5, TimeUnit.SECONDS)
      assert(Option(announced).exists(_.startsWith("announced sales on io-compute-")), announced)
    }
  }

  test("returns the failure of an asynchronous method in its Future") {
    withContext { context =>
      val failure = intercept[IllegalStateException](Await.result(context.getBean[ReportRenderer].fail(), 5.seconds))
      assertEquals(failure.getMessage, "cannot render")
    }
  }

  test("runs other methods on the calling thread") {
    withContext { context =>
      assertEquals(context.getBean[ReportRenderer].callerThread(), Thread.currentThread.getName)
    }
  }

  test("runs lifecycle methods on the bean behind the proxy") {
    val context  = contextOf()
    val renderer = target(context)
    context.close()
    assertEquals(List(renderer.events.poll(), renderer.events.poll()), List("started", "stopped"))
  }

  test("is not registered under its class") {
    withContext { context =>
      assertEquals(context.getBeanOption[PdfReportRenderer], None)
    }
  }

  test("rejects an @Async method that no trait declares") {
    val message = intercept[InvalidAsyncMethodException](process(classOf[AsyncWithoutTrait])).getMessage
    assert(message.startsWith("@Async method run of bean 'asyncWithoutTrait' is not declared in a trait"), message)
  }

  test("rejects an @Async method with another result") {
    val message = intercept[InvalidAsyncMethodException](process(classOf[AsyncWithResult])).getMessage
    assertEquals(message, "@Async method count of bean 'asyncWithResult' must return Unit or Future, not int")
  }

  private def target(context: ConfigurableApplicationContext): PdfReportRenderer =
    ProxiedBean.unwrap(context.getBean[ReportRenderer]) match {
      case renderer: PdfReportRenderer => renderer
      case other => fail(s"Unexpected bean behind the proxy: $other")
    }

  private def process(beanClass: Class[?]): Unit = {
    val _ = BeanDefinitionFactory.default.create(beanClass).map(processor.process)
  }

  private def withContext(body: ConfigurableApplicationContext => Unit): Unit = {
    val context = contextOf()
    try {
      body(context)
    } finally {
      context.close()
    }
  }

  private def contextOf(): ConfigurableApplicationContext =
    ApplicationContextBuilder.default.build(
      (ConcurrencyModule().components ++ List(classOf[PdfReportRenderer], classOf[ReportDesk]))
        .flatMap(BeanDefinitionFactory.default.create)
        .map(processor.process)
    )
}
