package dev.coilix.concurrency

import com.typesafe.config.ConfigFactory
import dev.coilix.concurrency.invalid.BadDuration
import dev.coilix.concurrency.invalid.BothTriggers
import dev.coilix.concurrency.invalid.NoTrigger
import dev.coilix.concurrency.invalid.WithArgument
import dev.coilix.concurrency.scheduled.FlakyRefresher
import dev.coilix.concurrency.scheduled.Ticker
import dev.coilix.config.ComponentProvider
import dev.coilix.config.Environment
import dev.coilix.config.TypesafeConfigTree
import dev.coilix.context.ApplicationContextBuilder
import dev.coilix.context.BeanDefinitionFactory
import dev.coilix.context.ConfigurableApplicationContext
import dev.coilix.context.RegisteredBean
import java.util.concurrent.TimeUnit
import munit.FunSuite

final class ScheduledMethodRegistrarTest extends FunSuite {
  private given ComponentProvider = ComponentProvider.builtIn

  test("runs a private @Scheduled method at a fixed rate on the scheduler") {
    withContext(classOf[Ticker]) { context =>
      val ticker = context.getBean[Ticker]
      assert(ticker.ticks.await(5, TimeUnit.SECONDS))
      assertEquals(ticker.thread.get, "coilix-scheduler-1")
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
    assertEquals(registrar.activeTasks, 1)
    context.close()
    assertEquals(registrar.activeTasks, 0)
  }

  test("rejects methods that cannot be scheduled") {
    val messages = List(classOf[BothTriggers], classOf[NoTrigger], classOf[WithArgument], classOf[BadDuration])
      .map(beanClass => intercept[InvalidScheduledMethodException](contextOf(beanClass)).getMessage)
    assertEquals(
      messages.map(_.takeWhile(_ != ':')),
      List(
        "@Scheduled method run of bean 'bothTriggers' sets both fixedRate and fixedDelay, set only one",
        "@Scheduled method run of bean 'noTrigger' sets neither fixedRate nor fixedDelay",
        "@Scheduled method run of bean 'withArgument' must not take parameters",
        "@Scheduled method run of bean 'badDuration' has an invalid fixedRate 'often'"
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

  private def contextOf(beanClass: Class[?]): ConfigurableApplicationContext = {
    val environment = Environment.of(TypesafeConfigTree(ConfigFactory.parseString("refresher.delay = 5ms")))
    ApplicationContextBuilder.default.build(
      (ConcurrencyModule().components :+ beanClass).flatMap(BeanDefinitionFactory.default.create),
      List(RegisteredBean.of[Environment]("environment", environment))
    )
  }
}
