package dev.coilix.testkit

import dev.coilix.boot.UnmatchedBeanReplacementException
import dev.coilix.common.GenericType
import dev.coilix.context.ApplicationContext
import dev.coilix.events.ApplicationFailed
import dev.coilix.events.ApplicationStarted
import dev.coilix.events.ContextReady
import dev.coilix.events.EventPublisher
import dev.coilix.testkit.fixture.FixtureApplication
import dev.coilix.testkit.fixture.Greeter
import dev.coilix.testkit.fixture.GuestBook
import dev.coilix.testkit.fixture.Journal
import dev.coilix.testkit.fixture.Reception
import dev.coilix.testkit.fixture.Uplink
import dev.coilix.testkit.fixture.Visit
import munit.FunSuite

final class DefaultTestContextFactoryTest extends FunSuite with CoilixContextOps {
  private val base = CoilixTestSetup.forApplication[FixtureApplication.type]

  private def welcome(using ApplicationContext): String = bean[Reception].welcome("Ann")

  test("builds the context of the application with its configuration files") {
    base.use(assertEquals(welcome, "Hello, Ann"))
  }

  test("properties win over configuration files") {
    base.withProperties("greeting.text" -> "Hi").use(assertEquals(welcome, "Hi, Ann"))
  }

  test("properties win over command-line options") {
    base.withArguments("--greeting.text=Hey").withProperties("greeting.text" -> "Hi").use(assertEquals(welcome, "Hi, Ann"))
  }

  test("properties win over system properties") {
    val key = "testkit.precedence-check"
    val _   = System.setProperty(key, "system")
    try {
      base.withProperties(key -> "test").use(assertEquals(property[String](key), Some("test")))
    } finally {
      val _ = System.clearProperty(key)
    }
  }

  test("registers the command-line arguments") {
    base.withArguments("--mode=fast").use(assertEquals(arguments.optionValue("mode"), Some("fast")))
  }

  test("profiles select the beans") {
    assertEquals(base.use(bean[Reception].channel), "quiet")
    assertEquals(base.withProfiles("loud").use(bean[Reception].channel), "loud")
  }

  test("a replaced bean is injected into the beans that depend on it") {
    val greeter: Greeter = name => s"Stub $name"
    base.replaceBean[Greeter](greeter).use {
      assertEquals(beans[Greeter], List(greeter))
      assertEquals(welcome, "Stub Ann")
    }
  }

  test("replacing a type the application does not define fails") {
    val uplink: Uplink = _ => ()
    intercept[UnmatchedBeanReplacementException](base.replaceBean[Uplink](uplink).use(()))
  }

  test("an added bean fills an optional dependency") {
    val uplink: Uplink = _ => ()
    assertEquals(base.use(bean[Reception].hasUplink), false)
    assertEquals(base.addBean[Uplink](uplink).use(bean[Reception].hasUplink), true)
  }

  test("runners do not run by default") {
    val journal = Journal()
    base.addBean[Journal](journal).use(assertEquals(journal.recorded, Nil))
  }

  test("withRunners runs the runners") {
    val journal = Journal()
    base.addBean[Journal](journal).withRunners.use(assertEquals(journal.recorded, List("runner")))
  }

  test("closes the context when a runner fails") {
    val journal = Journal()
    val setup   = base.addBean[Journal](journal).withProperties("fixture.fail-runner" -> "true").withRunners
    intercept[IllegalStateException](TestContextFactory.default.create(setup))
    assertEquals(journal.recorded, List("runner", "closed"))
  }

  test("publishes the startup events and records every event") {
    base.use {
      bean[EventPublisher].publish(Visit("Ann"))
      assertEquals(
        (recordedEvents[ContextReady].size, recordedEvents[ApplicationStarted].size, recordedEvents[Visit]),
        (1, 1, List(Visit("Ann")))
      )
    }
  }

  test("publishes ApplicationFailed when a runner fails") {
    val recorder = EventRecorder()
    val setup    = base
      .withBean(TestBean("failureRecorder", GenericType.of(classOf[EventRecorder]), recorder, replaces = false))
      .withProperties("fixture.fail-runner" -> "true")
      .withRunners
    intercept[IllegalStateException](TestContextFactory.default.create(setup))
    assertEquals(recorder.eventsOf[ApplicationFailed].map(_.failure.getMessage), List("runner failed"))
  }

  test("awaitEvents waits for asynchronous listeners") {
    base.use {
      bean[EventPublisher].publish(Visit("Ann"))
      assert(awaitEvents())
      assertEquals(bean[GuestBook].guests, List("Ann"))
    }
  }

  test("withSynchronousEvents runs asynchronous listeners in the thread of the test") {
    base.withSynchronousEvents.use {
      bean[EventPublisher].publish(Visit("Ann"))
      assertEquals(bean[GuestBook].threads, List(Thread.currentThread.getName))
    }
  }

  test("use closes the context after the block") {
    val journal = Journal()
    base.addBean[Journal](journal).use(assertEquals(journal.recorded, Nil))
    assertEquals(journal.recorded, List("closed"))
  }

  test("contexts with different properties stay independent") {
    base.withProperties("greeting.text" -> "One").use {
      val outer = welcome
      base.withProperties("greeting.text" -> "Two").use(assertEquals((outer, welcome), ("One, Ann", "Two, Ann")))
    }
  }

  test("scans only the given packages") {
    CoilixTestSetup.forPackages("dev.coilix.testkit.fixture").use(assertEquals(welcome, "Hello, Ann"))
  }
}
