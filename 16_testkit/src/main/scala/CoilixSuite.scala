package dev.coilix.testkit

import dev.coilix.context.ConfigurableApplicationContext
import munit.BaseFunSuite
import munit.Location

/**
 * A munit suite whose test bodies run inside a Coilix context given to them, with the helpers of
 * [[dev.coilix.testkit.CoilixContextOps]], so a test reads like the code it checks.
 *
 * ```scala
 * final class WelcomeMailTest extends munit.FunSuite with CoilixSuite {
 *   private given CoilixTestSetup = CoilixTestSetup
 *     .forApplication[NewsletterApplication.type]
 *     .withProperties("newsletter.smtp-host" -> "localhost")
 *
 *   coilixTest("creates the SMTP mailer") {
 *     assert(bean[Mailer].isInstanceOf[SmtpMailer])
 *   }
 *
 *   coilixTest("sends through a replaced mailer", summon[CoilixTestSetup].replaceBean[Mailer](RecordingMailer())) {
 *     bean[WelcomeMail].sendTo("ann@example.com")
 *   }
 * }
 * ```
 *
 * Each `coilixTest` builds its own context and closes it after the test. A context shared by the whole suite comes from
 * [[dev.coilix.testkit.CoilixFixtures.coilixSuiteContext]], and `shared.test(name) { ... }` runs a test inside it.
 */
trait CoilixSuite extends CoilixFixtures with CoilixContextOps {
  self: BaseFunSuite =>

  /** Registers a test that runs `body` in a fresh context built from the given setup. */
  def coilixTest(name: String)(body: ConfigurableApplicationContext ?=> Any)(using
    setup:    CoilixTestSetup,
    location: Location
  ): Unit =
    coilixTest(name, setup)(body)

  /** Registers a test that runs `body` in a fresh context built from `setup`. */
  def coilixTest(name: String, setup: CoilixTestSetup)(body: ConfigurableApplicationContext ?=> Any)(using
    location: Location
  ): Unit =
    self.test(name)(setup.use(body))

  extension (fixture: SuiteContextFixture) {

    /** Registers a test that runs `body` in the context shared by the suite. */
    def test(name: String)(body: ConfigurableApplicationContext ?=> Any)(using location: Location): Unit =
      self.test(name)(body(using fixture()))
  }
}
