package dev.coilix.testkit

import dev.coilix.boot.ApplicationArguments
import dev.coilix.boot.UnmatchedBeanReplacementException
import dev.coilix.context.ConfigurableApplicationContext
import dev.coilix.testkit.fixture.FixtureApplication
import dev.coilix.testkit.fixture.Greeter
import dev.coilix.testkit.fixture.Journal
import dev.coilix.testkit.fixture.Reception
import dev.coilix.testkit.fixture.Uplink
import munit.FunSuite
import scala.util.Using

final class DefaultTestContextFactoryTest extends FunSuite {
  private val base    = CoilixTestSetup.forApplication[FixtureApplication.type]
  private val factory = TestContextFactory.default

  private def withContext[A](setup: CoilixTestSetup)(check: ConfigurableApplicationContext => A): A =
    Using.resource(factory.create(setup))(check)

  test("builds the context of the application with its configuration files") {
    withContext(base)(context => assertEquals(context.getBean[Reception].welcome("Ann"), "Hello, Ann"))
  }

  test("properties win over configuration files") {
    withContext(base.withProperties("greeting.text" -> "Hi")) { context =>
      assertEquals(context.getBean[Reception].welcome("Ann"), "Hi, Ann")
    }
  }

  test("properties win over command-line options") {
    withContext(base.withArguments("--greeting.text=Hey").withProperties("greeting.text" -> "Hi")) { context =>
      assertEquals(context.getBean[Reception].welcome("Ann"), "Hi, Ann")
    }
  }

  test("properties win over system properties") {
    val _ = System.setProperty("greeting.text", "Ahoy")
    try {
      withContext(base.withProperties("greeting.text" -> "Hi")) { context =>
        assertEquals(context.getBean[Reception].welcome("Ann"), "Hi, Ann")
      }
    } finally {
      val _ = System.clearProperty("greeting.text")
    }
  }

  test("registers the command-line arguments") {
    withContext(base.withArguments("--mode=fast")) { context =>
      assertEquals(context.getBean[ApplicationArguments].optionValue("mode"), Some("fast"))
    }
  }

  test("profiles select the beans") {
    assertEquals(withContext(base)(_.getBean[Reception].channel), "quiet")
    assertEquals(withContext(base.withProfiles("loud"))(_.getBean[Reception].channel), "loud")
  }

  test("a replaced bean is injected into the beans that depend on it") {
    val greeter: Greeter = name => s"Stub $name"
    withContext(base.replaceBean[Greeter](greeter)) { context =>
      assertEquals(context.getBeansOfType[Greeter], List(greeter))
      assertEquals(context.getBean[Reception].welcome("Ann"), "Stub Ann")
    }
  }

  test("replacing a type the application does not define fails") {
    val uplink: Uplink = _ => ()
    intercept[UnmatchedBeanReplacementException](factory.create(base.replaceBean[Uplink](uplink)))
  }

  test("an added bean fills an optional dependency") {
    val uplink: Uplink = _ => ()
    assertEquals(withContext(base)(_.getBean[Reception].hasUplink), false)
    assertEquals(withContext(base.addBean[Uplink](uplink))(_.getBean[Reception].hasUplink), true)
  }

  test("runners do not run by default") {
    val journal = Journal()
    withContext(base.addBean[Journal](journal))(_ => assertEquals(journal.recorded, Nil))
  }

  test("withRunners runs the runners") {
    val journal = Journal()
    withContext(base.addBean[Journal](journal).withRunners)(_ => assertEquals(journal.recorded, List("runner")))
  }

  test("closes the context when a runner fails") {
    val journal = Journal()
    val setup   = base.addBean[Journal](journal).withProperties("fixture.fail-runner" -> "true").withRunners
    intercept[IllegalStateException](factory.create(setup))
    assertEquals(journal.recorded, List("runner", "closed"))
  }

  test("contexts with different properties stay independent") {
    val first  = factory.create(base.withProperties("greeting.text" -> "One"))
    val second = factory.create(base.withProperties("greeting.text" -> "Two"))
    try {
      assertEquals(
        (first.getBean[Reception].welcome("Ann"), second.getBean[Reception].welcome("Ann")),
        ("One, Ann", "Two, Ann")
      )
    } finally {
      first.close()
      second.close()
    }
  }

  test("scans only the given packages") {
    withContext(CoilixTestSetup.forPackages("dev.coilix.testkit.fixture")) { context =>
      assertEquals(context.getBean[Reception].welcome("Ann"), "Hello, Ann")
    }
  }
}
