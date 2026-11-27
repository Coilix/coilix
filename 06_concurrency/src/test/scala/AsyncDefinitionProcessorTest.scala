package dev.coilix.concurrency

import cats.effect.IO
import cats.effect.Resource
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
import munit.CatsEffectSuite
import scala.concurrent.duration.DurationInt

final class AsyncDefinitionProcessorTest extends CatsEffectSuite {
  private given ComponentProvider = ComponentProvider.builtIn

  private val processor = AsyncDefinitionProcessor()

  test("registers a bean with @Async methods under its trait and depends on the blocking executor and the tasks") {
    val definition = processor.process(BeanDefinitionFactory.default.create(classOf[PdfReportRenderer]).head)
    assertEquals(
      (definition.beanType, definition.dependencies.takeRight(2)),
      (
        GenericType.of(classOf[ReportRenderer]),
        List(
          Dependency.Required(GenericType.of(classOf[BlockingExecutor])),
          Dependency.Required(GenericType.of(classOf[AsyncTasks]))
        )
      )
    )
  }

  test("leaves other definitions as they are") {
    val definition = BeanDefinitionFactory.default.create(classOf[ReportDesk]).head
    assertEquals(processor.process(definition), definition)
  }

  test("calls the bean only when the IO of the call runs, on the compute pool") {
    context.use { context =>
      val call = context.getBean[ReportDesk].renderer.render("sales")
      IO(target(context).events.toArray.toList).flatMap { before =>
        call.map(result => assert(before == List("started") && result.startsWith("sales on io-compute-"), result))
      }
    }
  }

  test("runs a blocking @Async method on the blocking executor") {
    context.use { context =>
      context.getBean[ReportRenderer].archive("sales").map { result =>
        assert(result.startsWith("sales archived on coilix-blocking-"), result)
      }
    }
  }

  test("starts a method returning IO[Unit] on a fiber without waiting for it") {
    context.use { context =>
      val renderer = target(context)
      for {
        _         <- context.getBean[ReportRenderer].announce("sales")
        before    <- IO(renderer.events.toArray.toList)
        _         <- IO(renderer.release.countDown())
        _         <- IO(renderer.events.take())
        announced <- IO.interruptible(Option(renderer.events.poll(5, TimeUnit.SECONDS)))
      } yield {
        assertEquals(before, List("started"))
        assert(announced.exists(_.startsWith("announced sales on io-compute-")), announced)
      }
    }
  }

  test("returns the failure of an asynchronous method in its IO") {
    context.use { context =>
      context.getBean[ReportRenderer].fail().attempt.map { result =>
        assertEquals(result.left.map(_.getMessage), Left("cannot render"))
      }
    }
  }

  test("logs the failure of a method returning IO[Unit] and forgets its fiber") {
    context.use { context =>
      val tasks = context.getBean[AsyncTasks]
      context.getBean[ReportRenderer].failLater() *> tasks.running.iterateUntil(_ == 0).timeout(5.seconds).assertEquals(0)
    }
  }

  test("cancels the running fibers when the context closes") {
    IO(contextOf()).flatMap { context =>
      val renderer = target(context)
      for {
        _       <- context.getBean[ReportRenderer].hang()
        running <- context.getBean[AsyncTasks].running
        _       <- IO.blocking(context.close())
      } yield assertEquals((running, renderer.events.toArray.toList.contains("cancelled")), (1, true))
    }
  }

  test("skips a method returning IO[Unit] called after the context started closing") {
    IO(contextOf()).flatMap { context =>
      val tasks = context.getBean[AsyncTasks]
      IO.blocking(context.close()) *> context.getBean[ReportRenderer].hang() *> tasks.running.assertEquals(0)
    }
  }

  test("runs other methods on the calling thread") {
    context.use { context =>
      IO(assertEquals(context.getBean[ReportRenderer].callerThread(), Thread.currentThread.getName))
    }
  }

  test("runs lifecycle methods on the bean behind the proxy") {
    IO(contextOf()).flatMap { context =>
      val renderer = target(context)
      IO.blocking(context.close()).map { _ =>
        assertEquals(List(renderer.events.poll(), renderer.events.poll()), List("started", "stopped"))
      }
    }
  }

  test("is not registered under its class") {
    context.use(context => IO(assertEquals(context.getBeanOption[PdfReportRenderer], None)))
  }

  test("rejects an @Async method that no trait declares") {
    val message = intercept[InvalidAsyncMethodException](process(classOf[AsyncWithoutTrait])).getMessage
    assert(message.startsWith("@Async method run of bean 'asyncWithoutTrait' is not declared in a trait"), message)
  }

  test("rejects an @Async method that does not return IO") {
    val message = intercept[InvalidAsyncMethodException](process(classOf[AsyncWithResult])).getMessage
    assertEquals(message, "@Async method count of bean 'asyncWithResult' must return IO, not int")
  }

  private def target(context: ConfigurableApplicationContext): PdfReportRenderer =
    ProxiedBean.unwrap(context.getBean[ReportRenderer]) match {
      case renderer: PdfReportRenderer => renderer
      case other => fail(s"Unexpected bean behind the proxy: $other")
    }

  private def process(beanClass: Class[?]): Unit = {
    val _ = BeanDefinitionFactory.default.create(beanClass).map(processor.process)
  }

  private def context: Resource[IO, ConfigurableApplicationContext] =
    Resource.make(IO(contextOf()))(context => IO.blocking(context.close()))

  private def contextOf(): ConfigurableApplicationContext =
    ApplicationContextBuilder.default.build(
      (ConcurrencyModule().components ++ List(classOf[PdfReportRenderer], classOf[ReportDesk]))
        .flatMap(BeanDefinitionFactory.default.create)
        .map(processor.process)
    )
}
