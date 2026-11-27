package dev.coilix.concurrency

import cats.effect.unsafe.implicits.global
import com.typesafe.config.ConfigFactory
import dev.coilix.concurrency.invalid.BadDuration
import dev.coilix.concurrency.invalid.BothTriggers
import dev.coilix.concurrency.invalid.NoTrigger
import dev.coilix.concurrency.invalid.ReturnsUnit
import dev.coilix.concurrency.invalid.WithArgument
import dev.coilix.concurrency.scheduled.EventDrainWatcher
import dev.coilix.concurrency.scheduled.FlakyRefresher
import dev.coilix.concurrency.scheduled.Ticker
import dev.coilix.config.ComponentProvider
import dev.coilix.config.Environment
import dev.coilix.config.TypesafeConfigTree
import dev.coilix.context.ApplicationContextBuilder
import dev.coilix.context.BeanDefinitionFactory
import dev.coilix.context.ConfigurableApplicationContext
import dev.coilix.context.RegisteredBean
import dev.coilix.context.SynchronousLifecycle
import java.util.concurrent.TimeUnit
import munit.FunSuite

final class ScheduledMethodRegistrarTest extends FunSuite {
  private given ComponentProvider = ComponentProvider.builtIn

  test("runs a private @Scheduled method at a fixed rate on a fiber") {
    withContext(classOf[Ticker]) { context =>
      assert(context.getBean[Ticker].ticks.await(5, TimeUnit.SECONDS))
    }
  }

  test("resolves a placeholder and keeps running after a failed run") {
    withContext(classOf[FlakyRefresher]) { context =>
      assert(context.getBean[FlakyRefresher].recovered.await(5, TimeUnit.SECONDS))
    }
  }

  test("cancels every task when the context starts closing") {
    val context   = contextOf(classOf[Ticker])
    val registrar = context.getBean[ScheduledMethodRegistrar]
    assertEquals(SynchronousLifecycle.run(context, registrar.activeTasks), 1)
    context.close()
    assertEquals(registrar.activeTasks.unsafeRunSync(), 0)
  }

  test("cancels the tasks in the producers phase, before the events phase") {
    val context = contextOf(classOf[EventDrainWatcher], classOf[Ticker])
    context.close()
    assertEquals(context.getBean[EventDrainWatcher].activeTasksWhenDraining, Some(0))
  }

  test("rejects methods that cannot be scheduled") {
    val messages =
      List(classOf[BothTriggers], classOf[NoTrigger], classOf[WithArgument], classOf[BadDuration], classOf[ReturnsUnit])
        .map(beanClass => intercept[InvalidScheduledMethodException](contextOf(beanClass)).getMessage)
    assertEquals(
      messages.map(_.takeWhile(_ != ':')),
      List(
        "@Scheduled method run of bean 'bothTriggers' sets both fixedRate and fixedDelay, set only one",
        "@Scheduled method run of bean 'noTrigger' sets neither fixedRate nor fixedDelay",
        "@Scheduled method run of bean 'withArgument' must not take parameters",
        "@Scheduled method run of bean 'badDuration' has an invalid fixedRate 'often'",
        "@Scheduled method run of bean 'returnsUnit' must return IO[Unit], not void"
      )
    )
  }

  private def withContext(beanClass: Class[?])(body: ConfigurableApplicationContext => Unit): Unit = {
    val context = contextOf(beanClass)
    try {
      body(context)
    } finally {
      context.close()
    }
  }

  private def contextOf(beanClasses: Class[?]*): ConfigurableApplicationContext = {
    val environment = Environment.of(TypesafeConfigTree(ConfigFactory.parseString("refresher.delay = 5ms")))
    ApplicationContextBuilder.default.build(
      (ConcurrencyModule().components ++ beanClasses).flatMap(BeanDefinitionFactory.default.create),
      List(RegisteredBean.of[Environment]("environment", environment))
    )
  }
}
