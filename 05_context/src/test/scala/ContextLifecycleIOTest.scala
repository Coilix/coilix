package dev.coilix.context

import cats.effect.IO
import dev.coilix.config.ComponentProvider
import dev.coilix.context.lifecycle.ClosingAnnouncer
import dev.coilix.context.lifecycle.Connection
import dev.coilix.context.lifecycle.EventDrainer
import dev.coilix.context.lifecycle.FailingStart
import dev.coilix.context.lifecycle.Journal
import dev.coilix.context.lifecycle.StartWatcher
import munit.CatsEffectSuite

final class ContextLifecycleIOTest extends CatsEffectSuite {
  private given ComponentProvider = ComponentProvider.builtIn

  private def created(classes: Class[?]*): IO[ConfigurableApplicationContext] =
    ApplicationContextBuilder.default.create(Definitions.of((classOf[Journal] +: classes)*), Nil, None).allocated.map(_._1)

  test("create leaves the listeners to start in IO") {
    for {
      context <- created(classOf[StartWatcher])
      before  <- IO(context.getBean[Journal].all)
      _       <- context.start
      after   <- IO(context.getBean[Journal].all)
      _       <- context.shutdown
    } yield assertEquals((before, after), (Nil, List("started with applicationContext, journal, startWatcher")))
  }

  test("shutdown runs the closing listeners phase by phase and then destroys the beans") {
    for {
      context <- created(classOf[EventDrainer], classOf[StartWatcher], classOf[ClosingAnnouncer])
      _       <- context.start
      _       <- context.shutdown
    } yield assertEquals(
      (context.getBean[Journal].all.drop(1), context.isActive),
      (List("closing announced", "closing", "events drained", "watcher destroyed"), false)
    )
  }

  test("shutdown of a closed context does nothing") {
    for {
      context <- created(classOf[Connection])
      _       <- context.shutdown
      _       <- context.shutdown
    } yield assertEquals(context.getBean[Journal].all.count(_ == "connection closed"), 1)
  }

  test("a failing start fails the IO and a failing closing listener does not stop the shutdown") {
    for {
      context <- created(classOf[FailingStart], classOf[Connection])
      failure <- context.start.attempt
      _       <- context.shutdown
    } yield assertEquals(
      (failure.left.map(_.getMessage), context.getBean[Journal].all.last),
      (Left("cannot start"), "connection closed")
    )
  }
}
