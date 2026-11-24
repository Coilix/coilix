package dev.coilix.testkit.catseffect

import cats.effect.IO
import dev.coilix.context.ConfigurableApplicationContext
import dev.coilix.testkit.CoilixContextOps
import dev.coilix.testkit.CoilixTestSetup
import dev.coilix.testkit.catseffect.CoilixIOTestSetup.*
import munit.CatsEffectSuite
import munit.Location

/**
 * A munit-cats-effect suite whose test bodies return `IO` and run inside a Coilix context given to them, with the helpers of
 * [[dev.coilix.testkit.CoilixContextOps]].
 *
 * ```scala
 * final class OrderRepositoryTest extends CatsEffectSuite with CoilixIOSuite {
 *   private given CoilixTestSetup = CoilixTestSetup.forApplication[ShopApplication.type]
 *
 *   coilixTest("finds no order for an unknown id") {
 *     bean[OrderRepository].find(OrderId(7)).map(found => assertEquals(found, None))
 *   }
 * }
 * ```
 *
 * Each `coilixTest` builds its own context and closes it after the test. The runtime of the suite, `munitIORuntime`, is the
 * `ioRuntime` bean of the context, so the test and the beans run their effects on the same compute pool, as in a
 * [[dev.coilix.catseffect.CoilixIOApp]].
 */
trait CoilixIOSuite extends CoilixContextOps {
  self: CatsEffectSuite =>

  /** Registers a test that runs `body` in a fresh context built from the given setup. */
  def coilixTest(name: String)(body: ConfigurableApplicationContext ?=> IO[Any])(using
    setup:    CoilixTestSetup,
    location: Location
  ): Unit =
    coilixTest(name, setup)(body)

  /** Registers a test that runs `body` in a fresh context built from `setup`. */
  def coilixTest(name: String, setup: CoilixTestSetup)(body: ConfigurableApplicationContext ?=> IO[Any])(using
    location: Location
  ): Unit =
    self.test(name)(setup.withIORuntime(self.munitIORuntime).useIO(body))
}
