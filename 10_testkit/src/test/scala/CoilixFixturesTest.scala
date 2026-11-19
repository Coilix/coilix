package dev.coilix.testkit

import dev.coilix.testkit.fixture.FixtureApplication
import dev.coilix.testkit.fixture.Journal
import dev.coilix.testkit.fixture.Reception
import munit.FunSuite

final class CoilixFixturesTest extends FunSuite with CoilixFixtures {
  private val journal = Journal()
  private val setup   = CoilixTestSetup.forApplication[FixtureApplication.type].addBean[Journal](journal)
  private val each    = coilixContext(setup.withProperties("greeting.text" -> "Hi"))
  private val shared  = coilixSuiteContext(setup)

  override def munitFixtures = List(shared)

  each.test("gives each test its own context") { context =>
    assertEquals(context.getBean[Reception].welcome("Ann"), "Hi, Ann")
  }

  test("closes the context of a test after the test") {
    val entries = journal.recorded
    assert(entries.contains("closed"), entries)
  }

  test("shares one context in the suite") {
    assert(shared() eq shared())
    assertEquals(shared().getBean[Reception].welcome("Ann"), "Hello, Ann")
  }
}
