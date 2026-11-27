package dev.coilix.testkit

import dev.coilix.context.ConfigurableApplicationContext
import munit.Fixture
import scala.util.Try

/**
 * A munit fixture with one Coilix context for a whole suite, created by [[dev.coilix.testkit.CoilixFixtures.coilixSuiteContext]].
 *
 * The context is built in `beforeAll` and closed in `afterAll`. When building fails, the suite fails and nothing is closed.
 *
 * @param setup
 *   what the context contains
 * @param factory
 *   builds the context
 */
case class SuiteContextFixture(setup: CoilixTestSetup, factory: TestContextFactory)
    extends Fixture[ConfigurableApplicationContext]("coilix-context") {

  private lazy val context: ConfigurableApplicationContext = factory.create(setup)

  override def apply(): ConfigurableApplicationContext = context

  override def beforeAll(): Unit = {
    val _ = context
  }

  override def afterAll(): Unit =
    Try(context).foreach(_.close())
}
