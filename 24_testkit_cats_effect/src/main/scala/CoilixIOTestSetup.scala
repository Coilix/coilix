package dev.coilix.testkit.catseffect

import cats.effect.IO
import cats.effect.Resource
import cats.effect.unsafe.IORuntime
import dev.coilix.common.TypeInfo
import dev.coilix.context.ConfigurableApplicationContext
import dev.coilix.testkit.CoilixTestSetup
import dev.coilix.testkit.TestBean
import dev.coilix.testkit.TestContextFactory

/**
 * Uses a [[dev.coilix.testkit.CoilixTestSetup]] in `IO`: the test context becomes a `Resource`, and a test body that returns an
 * `IO` runs with the context given to it.
 *
 * ```scala
 * import dev.coilix.testkit.catseffect.CoilixIOTestSetup.*
 *
 * test("finds no order for an unknown id") {
 *   setup.useIO {
 *     bean[OrderRepository].find(OrderId(7)).map(found => assertEquals(found, None))
 *   }
 * }
 * ```
 */
object CoilixIOTestSetup {

  private val runtimeBeanName = "ioRuntime"

  extension (setup: CoilixTestSetup) {

    /**
     * Registers `runtime` as the `ioRuntime` bean, so the context runs its effects on the runtime of the test instead of creating
     * a runtime of its own. The context does not shut `runtime` down.
     */
    def withIORuntime(runtime: IORuntime): CoilixTestSetup =
      setup.withBean(TestBean(runtimeBeanName, TypeInfo[IORuntime].genericType, runtime, replaces = false))

    /** The context of this setup as a `Resource`: built in `IO.blocking` when acquired and closed when released. */
    def resource(using factory: TestContextFactory = TestContextFactory.default): Resource[IO, ConfigurableApplicationContext] =
      Resource.make(IO.blocking(factory.create(setup)))(context => IO.blocking(context.close()))

    /**
     * Builds the context, runs `body` with the context given to it and closes the context afterwards, also when `body` fails or
     * is cancelled.
     *
     * @return
     *   the result of `body`
     */
    def useIO[A](body: ConfigurableApplicationContext ?=> IO[A])(using
      factory: TestContextFactory = TestContextFactory.default
    ): IO[A] =
      resource(using factory).use(context => body(using context))
  }
}
