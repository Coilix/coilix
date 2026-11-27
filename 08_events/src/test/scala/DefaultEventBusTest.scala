package dev.coilix.events

import cats.effect.IO
import cats.effect.Resource
import com.typesafe.config.ConfigFactory
import dev.coilix.common.GenericType
import dev.coilix.concurrency.ConcurrencyModule
import dev.coilix.concurrency.RetryPolicy
import dev.coilix.config.ComponentProvider
import dev.coilix.config.Environment
import dev.coilix.config.TypesafeConfigTree
import dev.coilix.context.ApplicationContextBuilder
import dev.coilix.context.BeanDefinitionFactory
import dev.coilix.context.ConfigurableApplicationContext
import dev.coilix.context.ContextError
import dev.coilix.context.RegisteredBean
import dev.coilix.events.listeners.AsyncJournal
import dev.coilix.events.listeners.CancellingListener
import dev.coilix.events.listeners.ClosingWatcher
import dev.coilix.events.listeners.CustomerWatcher
import dev.coilix.events.listeners.EarlyPublisher
import dev.coilix.events.listeners.FailingClosingListener
import dev.coilix.events.listeners.FailingNotifier
import dev.coilix.events.listeners.FlakyNotifier
import dev.coilix.events.listeners.LazyAuditor
import dev.coilix.events.listeners.MailServer
import dev.coilix.events.listeners.OrderAudit
import dev.coilix.events.listeners.OrderJournal
import dev.coilix.events.listeners.SlowMailer
import dev.coilix.events.listeners.StrictValidators
import dev.coilix.events.listeners.StuckListener
import dev.coilix.events.model.Changed
import dev.coilix.events.model.Customer
import dev.coilix.events.model.OrderEvent
import dev.coilix.events.model.Product
import java.util.concurrent.TimeUnit
import munit.CatsEffectSuite
import scala.concurrent.duration.DurationInt

final class DefaultEventBusTest extends CatsEffectSuite {
  private given ComponentProvider = ComponentProvider.builtIn

  test("delivers an event to an annotated method and to a listener of its supertype before the publication ends") {
    contextOf(classOf[OrderJournal], classOf[OrderAudit]).use { context =>
      val bus = context.getBean[EventBus]
      (bus.publish(OrderEvent.Placed(1)) *> bus.publish(OrderEvent.Cancelled(2))).map { _ =>
        assertEquals(context.getBean[OrderJournal].placed.recorded, List(OrderEvent.Placed(1)))
        assertEquals(context.getBean[OrderAudit].events.recorded, List(OrderEvent.Placed(1), OrderEvent.Cancelled(2)))
      }
    }
  }

  test("matches type arguments of the event") {
    contextOf(classOf[CustomerWatcher]).use { context =>
      val bus = context.getBean[EventBus]
      (bus.publish(Changed(Customer("Ann"))) *> bus.publish(Changed(Product("Lamp")))).map(_ =>
        assertEquals(context.getBean[CustomerWatcher].changes.recorded, List(Changed(Customer("Ann"))))
      )
    }
  }

  test("runs every synchronous listener and fails with the first failure with the others suppressed") {
    contextOf(classOf[StrictValidators]).use { context =>
      val bus = context.getBean[EventBus]
      for {
        failure <- bus.publish(OrderEvent.Cancelled(1)).intercept[IllegalArgumentException]
        idle    <- bus.awaitIdle(5.seconds)
      } yield {
        assert(idle)
        assertEquals(
          (failure.getMessage, failure.getSuppressed.toList.map(_.getMessage)),
          ("customer rejected", List("stock rejected"))
        )
        assertEquals(context.getBean[StrictValidators].warehouse.recorded, Nil)
      }
    }
  }

  test("ends the publication before an asynchronous listener finishes, which runs on a fiber of its own") {
    contextOf(classOf[SlowMailer]).use { context =>
      val bus    = context.getBean[EventBus]
      val mailer = context.getBean[SlowMailer]
      for {
        _       <- bus.publish(OrderEvent.Placed(1))
        started <- IO.interruptible(mailer.started.await(5, TimeUnit.SECONDS))
        before  <- bus.pendingDeliveries
        _       <- IO(mailer.release.countDown())
        idle    <- bus.awaitIdle(5.seconds)
      } yield {
        assert(started && idle)
        assertEquals(before, 1)
        assertEquals(mailer.sent.recorded, List((1, true)))
      }
    }
  }

  test("retries an asynchronous listener with the configured policy") {
    val config = "coilix.events.listeners.flakyNotifier.onPlaced.retry { max-attempts = 3, initial-delay = 10ms, jitter = 0 }"
    contextOf(config, classOf[FlakyNotifier]).use { context =>
      val notifier = context.getBean[FlakyNotifier]
      for {
        _         <- context.getBean[EventBus].publish(OrderEvent.Placed(1))
        delivered <- IO.interruptible(notifier.delivered.await(5, TimeUnit.SECONDS))
        pending   <- context.getBean[EventBus].awaitIdle(5.seconds) *> context.getBean[EventBus].pendingDeliveries
      } yield assertEquals((delivered, notifier.attempts.get, pending), (true, 3, 0))
    }
  }

  test("tries an asynchronous listener once without a retry policy and keeps the failure from the publisher") {
    contextOf(classOf[FailingNotifier]).use { context =>
      val bus = context.getBean[EventBus]
      (bus.publish(OrderEvent.Placed(1)) *> bus.awaitIdle(5.seconds)).map { idle =>
        assert(idle)
        assertEquals(context.getBean[FailingNotifier].attempts.get, 1)
      }
    }
  }

  test("runs asynchronous listeners in the fiber of the publisher in synchronous mode") {
    contextOf("coilix.events.synchronous = true", classOf[AsyncJournal], classOf[FailingNotifier]).use { context =>
      val bus = context.getBean[EventBus]
      (bus.publish(OrderEvent.Placed(1)) *> bus.pendingDeliveries).map { pending =>
        assertEquals(
          (context.getBean[AsyncJournal].orders.recorded, context.getBean[FailingNotifier].attempts.get, pending),
          (List(1), 1, 0)
        )
      }
    }
  }

  test("retries at once without waiting in synchronous mode") {
    val config =
      "coilix.events { synchronous = true, listeners.flakyNotifier.onPlaced.retry { max-attempts = 3, initial-delay = 1m } }"
    contextOf(config, classOf[FlakyNotifier]).use { context =>
      context.getBean[EventBus].publish(OrderEvent.Placed(1)).map(_ => assertEquals(context.getBean[FlakyNotifier].attempts.get, 3))
    }
  }

  test("publishes ContextClosing on closing and waits for its asynchronous listeners") {
    contextOf(classOf[ClosingWatcher]).allocated.flatMap { (context, close) =>
      close.map(_ => assertEquals(context.getBean[ClosingWatcher].sawBean.recorded, List(true)))
    }
  }

  test("closes the context when a listener of ContextClosing fails") {
    contextOf(classOf[FailingClosingListener], classOf[SlowMailer]).allocated.flatMap { (context, close) =>
      close.map(_ => assert(!context.getBean[MailServer].isOpen))
    }
  }

  test("does not create a lazy bean to look for listeners in it") {
    contextOf(classOf[LazyAuditor]).use { context =>
      context.getBean[EventBus].listeners.map { listeners =>
        assertEquals((context.createdBeans.exists(_.beanName == "lazyAuditor"), listeners.map(_.name)), (false, Nil))
      }
    }
  }

  test("delivers events published before the context started") {
    contextOf(classOf[EarlyPublisher], classOf[OrderAudit]).use { context =>
      IO(assertEquals(context.getBean[OrderAudit].events.recorded, List(OrderEvent.Placed(0))))
    }
  }

  test("lists the listeners with their event types and retry policies") {
    val config = "coilix.events.listeners.slowMailer.onPlaced.retry.max-attempts = 2"
    contextOf(config, classOf[OrderAudit], classOf[SlowMailer]).use { context =>
      context.getBean[EventBus].listeners.map { listeners =>
        assertEquals(
          listeners,
          List(
            EventListenerInfo("orderAudit.onEvent", GenericType.of(classOf[OrderEvent]), false, RetryPolicy.none),
            EventListenerInfo("slowMailer.onPlaced", GenericType.of(classOf[OrderEvent.Placed]), true, RetryPolicy(maxAttempts = 2))
          )
        )
      }
    }
  }

  test("waits on closing for running asynchronous listeners before beans are destroyed") {
    contextOf(classOf[SlowMailer]).allocated.flatMap { (context, close) =>
      val mailer = context.getBean[SlowMailer]
      for {
        _ <- context.getBean[EventBus].publish(OrderEvent.Placed(1))
        _ <- IO.interruptible(mailer.started.await(5, TimeUnit.SECONDS))
        _ <- (IO.sleep(200.millis) *> IO(mailer.release.countDown())).start
        _ <- close
      } yield {
        assertEquals(mailer.sent.recorded, List((1, true)))
        assert(!context.getBean[MailServer].isOpen)
      }
    }
  }

  test("delivers events that listeners publish while the context is closing") {
    contextOf(classOf[CancellingListener], classOf[OrderAudit]).allocated.flatMap { (context, close) =>
      (context.getBean[EventBus].publish(OrderEvent.Placed(5)) *> close).map(_ =>
        assertEquals(context.getBean[OrderAudit].events.recorded, List(OrderEvent.Placed(5), OrderEvent.Cancelled(5)))
      )
    }
  }

  test("rejects events from outside listeners while the context is closing and after it closed") {
    contextOf(classOf[SlowMailer]).allocated.flatMap { (context, close) =>
      val bus    = context.getBean[EventBus]
      val mailer = context.getBean[SlowMailer]
      for {
        _       <- bus.publish(OrderEvent.Placed(1))
        _       <- IO.interruptible(mailer.started.await(5, TimeUnit.SECONDS))
        closer  <- close.start
        closing <- rejection(bus).timeout(5.seconds)
        _       <- IO(mailer.release.countDown())
        _       <- closer.joinWithNever
        closed  <- bus.publish(OrderEvent.Placed(3)).intercept[EventBusClosedException]
        refused <- bus.tryPublish(OrderEvent.Placed(4)).value
      } yield {
        assertEquals(closing, PublishError.Closing(GenericType.of(classOf[OrderEvent.Cancelled])))
        assertEquals(
          closed.getMessage,
          s"Cannot publish an event of type ${classOf[OrderEvent.Placed].getName}, the context is closed"
        )
        assertEquals(refused, Left(PublishError.Closed(GenericType.of(classOf[OrderEvent.Placed]))))
      }
    }
  }

  test("cancels asynchronous listeners that run longer than the shutdown timeout") {
    contextOf("coilix.events.shutdown-timeout = 100ms", classOf[StuckListener]).allocated.flatMap { (context, close) =>
      val stuck = context.getBean[StuckListener]
      for {
        _ <- context.getBean[EventBus].publish(OrderEvent.Placed(1))
        _ <- IO.interruptible(stuck.started.await(5, TimeUnit.SECONDS))
        _ <- close
      } yield assert(stuck.cancelled.get)
    }
  }

  private def rejection(bus: EventBus): IO[PublishError] =
    bus.tryPublish(OrderEvent.Cancelled(2)).value.flatMap {
      case Left(error) => IO.pure(error)
      case Right(_)    => IO.sleep(5.millis) *> rejection(bus)
    }

  private def contextOf(beanClasses: Class[?]*): Resource[IO, ConfigurableApplicationContext] =
    contextOf("", beanClasses*)

  private def contextOf(config: String, beanClasses: Class[?]*): Resource[IO, ConfigurableApplicationContext] = {
    val environment = Environment.of(TypesafeConfigTree(ConfigFactory.parseString(config)))
    ContextError
      .raised(
        ApplicationContextBuilder.default.create(
          (ConcurrencyModule().components ++ EventsModule().components ++ beanClasses ++ dependencies(beanClasses))
            .distinct
            .flatMap(BeanDefinitionFactory.default.create),
          List(RegisteredBean.of[Environment]("environment", environment)),
          ConcurrencyModule().effectInterpreter
        )
      )
      .evalTap(_.start)
  }

  private def dependencies(beanClasses: Seq[Class[?]]): Seq[Class[?]] =
    if (beanClasses.contains(classOf[SlowMailer])) Seq(classOf[MailServer]) else Nil
}
