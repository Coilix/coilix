package dev.coilix.testkit

import dev.coilix.testkit.fixture.FixtureApplication
import dev.coilix.testkit.fixture.Notifier
import dev.coilix.testkit.fixture.Reception
import dev.coilix.testkit.fixture.TemplateGreeter
import dev.coilix.testkit.fixture.Uplink
import munit.FunSuite

final class CoilixSuiteTest extends FunSuite with CoilixSuite {
  private given CoilixTestSetup = CoilixTestSetup.forApplication[FixtureApplication.type]

  private val shared = coilixSuiteContext(summon[CoilixTestSetup].withProperties("greeting.text" -> "Shared"))

  override def munitFixtures = List(shared)

  coilixTest("runs the test in a context built from the given setup") {
    assertEquals(bean[Reception].welcome("Ann"), "Hello, Ann")
  }

  coilixTest("runs the test in a context built from an explicit setup", summon[CoilixTestSetup].withProfiles("loud")) {
    assertEquals(bean[Notifier].channel, "loud")
  }

  coilixTest("reads configuration values") {
    assertEquals((property[String]("greeting.text"), property[String]("missing.key")), (Some("Hello"), None))
  }

  coilixTest("finds beans by type") {
    assertEquals((beanOption[Uplink], beans[TemplateGreeter].size), (None, 1))
  }

  coilixTest("gives the context itself") {
    assert(context.beanNames.contains("reception"))
  }

  shared.test("runs the test in the context shared by the suite") {
    assertEquals(bean[Reception].welcome("Ann"), "Shared, Ann")
  }

  shared.test("shares the same context between tests") {
    assert(context eq shared())
  }
}
