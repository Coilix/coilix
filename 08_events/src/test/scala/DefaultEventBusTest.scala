package dev.coilix.events

import com.typesafe.config.ConfigFactory
import dev.coilix.common.GenericType
import dev.coilix.concurrency.BlockingExecutor
import dev.coilix.concurrency.ConcurrencyModule
import dev.coilix.concurrency.RetryPolicy
import dev.coilix.concurrency.TaskExecutor
import dev.coilix.config.ComponentProvider
import dev.coilix.config.Environment
import dev.coilix.config.TypesafeConfigTree
import dev.coilix.context.ApplicationContextBuilder
import dev.coilix.context.BeanDefinitionFactory
import dev.coilix.context.ConfigurableApplicationContext
import dev.coilix.context.RegisteredBean
import dev.coilix.context.SynchronousLifecycle
import dev.coilix.events.listeners.ArchivingListener
import dev.coilix.events.listeners.AsyncJournal
import dev.coilix.events.listeners.CancellingListener
import dev.coilix.events.listeners.ClosingWatcher
import dev.coilix.events.listeners.CustomerWatcher
import dev.coilix.events.listeners.EarlyPublisher
import dev.coilix.events.listeners.ExhaustedListener
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
import munit.FunSuite
import scala.annotation.tailrec
import scala.concurrent.duration.DurationInt
import scala.util.Try

final class DefaultEventBusTest extends FunSuite {
  private given ComponentProvider = ComponentProvider.builtIn

  test("delivers an event to an annotated method and to a listener of its supertype in the publishing thread") {
    withContext(classOf[OrderJournal], classOf[OrderAudit]) { context =>
      val bus     = context.getBean[EventBus]
      bus.publish(OrderEvent.Placed(1))
      bus.publish(OrderEvent.Cancelled(2))
      val journal = context.getBean[OrderJournal]
      assertEquals(journal.placed.recorded, List(OrderEvent.Placed(1)))
      assertEquals(journal.threads.recorded, List(Thread.currentThread.getName))
      assertEquals(context.getBean[OrderAudit].events.recorded, List(OrderEvent.Placed(1), OrderEvent.Cancelled(2)))
    }
  }

  test("matches type arguments of the event") {
    withContext(classOf[CustomerWatcher]) { context =>
      val bus = context.getBean[EventBus]
      bus.publish(Changed(Customer("Ann")))
      bus.publish(Changed(Product("Lamp")))
      assertEquals(context.getBean[CustomerWatcher].changes.recorded, List(Changed(Customer("Ann"))))
    }
  }

  test("runs every synchronous listener and throws the first failure with the others suppressed") {
    withContext(classOf[StrictValidators]) { context =>
      val bus     = context.getBean[EventBus]
      val failure = intercept[IllegalArgumentException](bus.publish(OrderEvent.Cancelled(1)))
      assert(bus.awaitIdle(5.seconds))
      assertEquals(
        (failure.getMessage, failure.getSuppressed.toList.map(_.getMessage)),
        ("customer rejected", List("stock rejected"))
      )
      assertEquals(context.getBean[StrictValidators].warehouse.recorded, Nil)
    }
  }

  test("returns before an asynchronous listener finishes, which runs on the task executor") {
    withContext(classOf[SlowMailer]) { context =>
      val bus    = context.getBean[EventBus]
      val mailer = context.getBean[SlowMailer]
      bus.publish(OrderEvent.Placed(1))
      assert(mailer.started.await(5, TimeUnit.SECONDS))
      assertEquals((mailer.sent.recorded, bus.pendingDeliveries), (Nil, 1))
      mailer.release.countDown()
      assert(bus.awaitIdle(5.seconds))
      assertEquals(mailer.sent.recorded, List((1, true)))
      assert(mailer.threads.recorded.forall(_.startsWith("io-compute-")))
    }
  }

  test("runs an asynchronous listener marked blocking on the blocking executor") {
    withContext(classOf[ArchivingListener]) { context =>
      context.getBean[EventBus].publish(OrderEvent.Placed(1))
      val listener = context.getBean[ArchivingListener]
      assert(listener.archived.await(5, TimeUnit.SECONDS))
      assert(listener.threads.recorded.forall(_.startsWith("coilix-blocking-")), listener.threads.recorded)
    }
  }

  test("retries an asynchronous listener with the configured policy") {
    val config = "coilix.events.listeners.flakyNotifier.onPlaced.retry { max-attempts = 3, initial-delay = 10ms, jitter = 0 }"
    withContext(config, classOf[FlakyNotifier]) { context =>
      context.getBean[EventBus].publish(OrderEvent.Placed(1))
      val notifier = context.getBean[FlakyNotifier]
      assert(notifier.delivered.await(5, TimeUnit.SECONDS))
      assertEquals(notifier.attempts.get, 3)
    }
  }

  test("retries an asynchronous listener when the scheduler runs the retry before scheduling returns") {
    val config = "coilix.events.listeners.flakyNotifier.onPlaced.retry { max-attempts = 3, initial-delay = 10ms, jitter = 0 }"
    withContext(config, classOf[FlakyNotifier]) { context =>
      val bus = DefaultEventBus(
        context.getBean[TaskExecutor],
        context.getBean[BlockingExecutor],
        ImmediateScheduler(),
        context.getBean[EventsSettings]
      )
      bus.onContextStarted(context).unsafeRunSync()(using SynchronousLifecycle.runtimeOf(context))
      bus.publish(OrderEvent.Placed(1))
      assert(context.getBean[FlakyNotifier].delivered.await(5, TimeUnit.SECONDS))
      assert(bus.awaitIdle(5.seconds))
      assertEquals((context.getBean[FlakyNotifier].attempts.get, bus.pendingDeliveries), (3, 0))
      bus.onContextClosing.unsafeRunSync()(using SynchronousLifecycle.runtimeOf(context))
    }
  }

  test("tries an asynchronous listener once without a retry policy and keeps the failure from the publisher") {
    withContext(classOf[FailingNotifier]) { context =>
      val bus = context.getBean[EventBus]
      bus.publish(OrderEvent.Placed(1))
      assert(bus.awaitIdle(5.seconds))
      assertEquals(context.getBean[FailingNotifier].attempts.get, 1)
    }
  }

  test("runs asynchronous listeners in the publishing thread in synchronous mode") {
    withContext("coilix.events.synchronous = true", classOf[AsyncJournal], classOf[FailingNotifier]) { context =>
      val bus = context.getBean[EventBus]
      bus.publish(OrderEvent.Placed(1))
      assertEquals(
        (context.getBean[AsyncJournal].threads.recorded, context.getBean[FailingNotifier].attempts.get, bus.pendingDeliveries),
        (List(Thread.currentThread.getName), 1, 0)
      )
    }
  }

  test("retries at once without waiting in synchronous mode") {
    val config =
      "coilix.events { synchronous = true, listeners.flakyNotifier.onPlaced.retry { max-attempts = 3, initial-delay = 1m } }"
    withContext(config, classOf[FlakyNotifier]) { context =>
      context.getBean[EventBus].publish(OrderEvent.Placed(1))
      assertEquals(context.getBean[FlakyNotifier].attempts.get, 3)
    }
  }

  test("publishes ContextClosing on closing and waits for its asynchronous listeners") {
    val context = contextOf("", classOf[ClosingWatcher])
    context.close()
    assertEquals(context.getBean[ClosingWatcher].sawBean.recorded, List(true))
  }

  test("closes the context when a listener of ContextClosing fails") {
    val context = contextOf("", classOf[FailingClosingListener], classOf[SlowMailer])
    context.close()
    assert(!context.getBean[MailServer].isOpen)
  }

  test("ends an asynchronous delivery without retries after a fatal error, so closing does not wait for it") {
    val config = "coilix.fatal-errors.action = log, coilix.events.listeners.exhaustedListener.onPlaced.retry.max-attempts = 3"
    withContext(config, classOf[ExhaustedListener]) { context =>
      val bus = context.getBean[EventBus]
      bus.publish(OrderEvent.Placed(1))
      assert(bus.awaitIdle(5.seconds))
      assertEquals((context.getBean[ExhaustedListener].attempts.get, bus.pendingDeliveries), (1, 0))
    }
  }

  test("does not create a lazy bean to look for listeners in it") {
    withContext(classOf[LazyAuditor]) { context =>
      assertEquals(
        (context.createdBeans.exists(_.beanName == "lazyAuditor"), context.getBean[EventBus].listeners.map(_.name)),
        (false, Nil)
      )
    }
  }

  test("delivers events published before the context started") {
    withContext(classOf[EarlyPublisher], classOf[OrderAudit]) { context =>
      assertEquals(context.getBean[OrderAudit].events.recorded, List(OrderEvent.Placed(0)))
    }
  }

  test("lists the listeners with their event types and retry policies") {
    val config = "coilix.events.listeners.slowMailer.onPlaced.retry.max-attempts = 2"
    withContext(config, classOf[OrderAudit], classOf[SlowMailer]) { context =>
      assertEquals(
        context.getBean[EventBus].listeners,
        List(
          EventListenerInfo("orderAudit.onEvent", GenericType.of(classOf[OrderEvent]), false, RetryPolicy.none),
          EventListenerInfo("slowMailer.onPlaced", GenericType.of(classOf[OrderEvent.Placed]), true, RetryPolicy(maxAttempts = 2))
        )
      )
    }
  }

  test("waits on closing for running asynchronous listeners before beans are destroyed") {
    val context  = contextOf("", classOf[SlowMailer])
    val mailer   = context.getBean[SlowMailer]
    context.getBean[EventBus].publish(OrderEvent.Placed(1))
    assert(mailer.started.await(5, TimeUnit.SECONDS))
    val releaser = background {
      Thread.sleep(200)
      mailer.release.countDown()
    }
    context.close()
    releaser.join()
    assertEquals(mailer.sent.recorded, List((1, true)))
    assert(!context.getBean[MailServer].isOpen)
  }

  test("delivers events that listeners publish while the context is closing") {
    val context = contextOf("", classOf[CancellingListener], classOf[OrderAudit])
    context.getBean[EventBus].publish(OrderEvent.Placed(5))
    context.close()
    assertEquals(context.getBean[OrderAudit].events.recorded, List(OrderEvent.Placed(5), OrderEvent.Cancelled(5)))
  }

  test("rejects events from outside listeners while the context is closing and after it closed") {
    val context = contextOf("", classOf[SlowMailer])
    val bus     = context.getBean[EventBus]
    val mailer  = context.getBean[SlowMailer]
    bus.publish(OrderEvent.Placed(1))
    assert(mailer.started.await(5, TimeUnit.SECONDS))
    val closer  = background(context.close())
    val closing = rejection(bus, System.nanoTime + 5.seconds.toNanos)
    mailer.release.countDown()
    closer.join()
    val closed  = intercept[EventBusClosedException](bus.publish(OrderEvent.Placed(3))).getMessage
    assertEquals(
      (closing.map(_.getMessage), closed),
      (
        Some(s"Cannot publish an event of type ${classOf[OrderEvent.Placed].getName}, the context is closing"),
        s"Cannot publish an event of type ${classOf[OrderEvent.Placed].getName}, the context is closed"
      )
    )
  }

  test("interrupts asynchronous listeners that run longer than the shutdown timeout") {
    val context = contextOf("coilix.events.shutdown-timeout = 100ms", classOf[StuckListener])
    val stuck   = context.getBean[StuckListener]
    context.getBean[EventBus].publish(OrderEvent.Placed(1))
    assert(stuck.started.await(5, TimeUnit.SECONDS))
    context.close()
    assert(interrupted(stuck, System.nanoTime + 5.seconds.toNanos))
  }

  private def background(body: => Unit): Thread = {
    val thread = Thread(() => body)
    thread.start()
    thread
  }

  @tailrec
  private def rejection(bus: EventBus, deadline: Long): Option[EventBusClosedException] =
    Try(bus.publish(OrderEvent.Placed(2))).failed.toOption match {
      case Some(failure: EventBusClosedException) => Some(failure)
      case _ if System.nanoTime > deadline        => None
      case _                                      =>
        Thread.sleep(5)
        rejection(bus, deadline)
    }

  @tailrec
  private def interrupted(stuck: StuckListener, deadline: Long): Boolean =
    if (stuck.interrupted.get) true
    else if (System.nanoTime > deadline) false
    else {
      Thread.sleep(5)
      interrupted(stuck, deadline)
    }

  private def withContext(beanClasses: Class[?]*)(body: ConfigurableApplicationContext => Unit): Unit =
    withContext("", beanClasses*)(body)

  private def withContext(config: String, beanClasses: Class[?]*)(body: ConfigurableApplicationContext => Unit): Unit = {
    val context = contextOf(config, beanClasses*)
    try {
      body(context)
    } finally {
      context.close()
    }
  }

  private def contextOf(config: String, beanClasses: Class[?]*): ConfigurableApplicationContext = {
    val environment = Environment.of(TypesafeConfigTree(ConfigFactory.parseString(config)))
    ApplicationContextBuilder.default.build(
      (ConcurrencyModule().components ++ EventsModule().components ++ beanClasses ++ dependencies(beanClasses))
        .distinct
        .flatMap(BeanDefinitionFactory.default.create),
      List(RegisteredBean.of[Environment]("environment", environment))
    )
  }

  private def dependencies(beanClasses: Seq[Class[?]]): Seq[Class[?]] =
    if (beanClasses.contains(classOf[SlowMailer])) Seq(classOf[MailServer]) else Nil
}
