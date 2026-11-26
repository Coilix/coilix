package dev.coilix.boot

import cats.effect.IO
import cats.effect.Resource
import dev.coilix.boot.startup.BrokenFailureListener
import dev.coilix.boot.startup.StartupJournal
import dev.coilix.concurrency.ConcurrencyModule
import dev.coilix.config.ComponentProvider
import dev.coilix.context.ApplicationContextBuilder
import dev.coilix.context.BeanDefinitionFactory
import dev.coilix.context.ConfigurableApplicationContext
import dev.coilix.context.ContextError
import dev.coilix.events.EventsModule
import munit.CatsEffectSuite

final class StartupEventsTest extends CatsEffectSuite {
  private given ComponentProvider = ComponentProvider.builtIn

  test("publishes ContextReady before the runners and ApplicationStarted after them") {
    contextOf(withEvents(classOf[StartupJournal])).use { context =>
      val journal = context.getBean[StartupJournal]
      StartupEvents.around(context)(IO(journal.record("runners"))).map(_ =>
        assertEquals(journal.recorded, List("ready:true", "runners", "started"))
      )
    }
  }

  test("publishes ApplicationFailed and fails with the failure of a runner, leaving the context open") {
    contextOf(withEvents(classOf[StartupJournal])).use { context =>
      StartupEvents.around(context)(IO.raiseError(IllegalStateException("no database"))).attempt.map(result =>
        assertEquals(
          (result.left.map(_.getMessage), context.getBean[StartupJournal].recorded, context.isActive),
          (Left("no database"), List("ready:true", "failed:no database"), true)
        )
      )
    }
  }

  test("fails with the startup failure when a listener of ApplicationFailed fails") {
    contextOf(withEvents(classOf[StartupJournal], classOf[BrokenFailureListener])).use { context =>
      StartupEvents.around(context)(IO.raiseError(IllegalArgumentException("bad input"))).attempt.map(result =>
        assertEquals(result.left.map(_.getMessage), Left("bad input"))
      )
    }
  }

  test("only runs the runners without the events module") {
    contextOf(List(classOf[StartupJournal])).use { context =>
      val journal = context.getBean[StartupJournal]
      StartupEvents.around(context)(IO(journal.record("runners"))).map(_ => assertEquals(journal.recorded, List("runners")))
    }
  }

  private def withEvents(beanClasses: Class[?]*): List[Class[?]] =
    ConcurrencyModule().components ++ EventsModule().components ++ beanClasses

  private def contextOf(beanClasses: List[Class[?]]): Resource[IO, ConfigurableApplicationContext] =
    ContextError.raised(ApplicationContextBuilder.default.create(
      beanClasses.flatMap(BeanDefinitionFactory.default.create),
      Nil,
      None
    ))
      .evalTap(_.start)
}
