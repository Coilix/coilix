package dev.coilix.boot

import dev.coilix.boot.startup.BrokenFailureListener
import dev.coilix.boot.startup.StartupJournal
import dev.coilix.concurrency.ConcurrencyModule
import dev.coilix.config.ComponentProvider
import dev.coilix.context.ApplicationContextBuilder
import dev.coilix.context.BeanDefinitionFactory
import dev.coilix.context.ConfigurableApplicationContext
import dev.coilix.events.EventsModule
import munit.FunSuite

final class StartupEventsTest extends FunSuite {
  private given ComponentProvider = ComponentProvider.builtIn

  test("publishes ContextReady before the runners and ApplicationStarted after them") {
    val context = contextOf(classOf[StartupJournal])
    val journal = context.getBean[StartupJournal]
    try {
      val started = StartupEvents.around(context)(journal.record("runners"))
      assertEquals((started eq context, journal.recorded), (true, List("ready:true", "runners", "started")))
    } finally {
      context.close()
    }
  }

  test("publishes ApplicationFailed, closes the context and rethrows when a runner fails") {
    val context = contextOf(classOf[StartupJournal])
    val failure = intercept[IllegalStateException](StartupEvents.around(context)(throw IllegalStateException("no database")))
    assertEquals(
      (failure.getMessage, context.getBean[StartupJournal].recorded, context.isActive),
      ("no database", List("ready:true", "failed:no database"), false)
    )
  }

  test("rethrows the startup failure when a listener of ApplicationFailed fails") {
    val context = contextOf(classOf[StartupJournal], classOf[BrokenFailureListener])
    val failure = intercept[IllegalArgumentException](StartupEvents.around(context)(throw IllegalArgumentException("bad input")))
    assertEquals((failure.getMessage, context.isActive), ("bad input", false))
  }

  test("only runs the runners without the events module") {
    val context = ApplicationContextBuilder.default.build(BeanDefinitionFactory.default.create(classOf[StartupJournal]))
    val journal = context.getBean[StartupJournal]
    try {
      StartupEvents.around(context)(journal.record("runners"))
      assertEquals(journal.recorded, List("runners"))
    } finally {
      context.close()
    }
  }

  private def contextOf(beanClasses: Class[?]*): ConfigurableApplicationContext =
    ApplicationContextBuilder.default.build(
      (ConcurrencyModule().components ++ EventsModule().components ++ beanClasses).flatMap(BeanDefinitionFactory.default.create)
    )
}
