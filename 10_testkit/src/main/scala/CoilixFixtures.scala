package dev.coilix.testkit

import dev.coilix.context.ConfigurableApplicationContext
import munit.BaseFunSuite

/**
 * munit fixtures that build a Coilix context for a test and close it afterwards, which also runs the `@PreDestroy` callbacks.
 *
 * Mix it into a munit suite. A fresh context for each test:
 *
 * ```scala
 * final class InvoiceServiceTest extends munit.FunSuite with CoilixFixtures {
 *   private val context = coilixContext(
 *     CoilixTestSetup
 *       .forApplication[ShopApplication.type]
 *       .replaceBean[PaymentGateway](AcceptingGateway())
 *   )
 *
 *   context.test("issues an invoice for a paid order") { ctx =>
 *     assertEquals(ctx.getBean[InvoiceService].issue(OrderId("O-1")).status, InvoiceStatus.Issued)
 *   }
 * }
 * ```
 *
 * One context for the whole suite, registered in `munitFixtures`:
 *
 * ```scala
 * final class CatalogTest extends munit.FunSuite with CoilixFixtures {
 *   private val context = coilixSuiteContext(CoilixTestSetup.forApplication[ShopApplication.type])
 *
 *   override def munitFixtures = List(context)
 *
 *   test("lists the products") {
 *     assert(context().getBean[Catalog].products.nonEmpty)
 *   }
 * }
 * ```
 */
trait CoilixFixtures {
  self: BaseFunSuite =>

  /** A fixture that builds a context before each test and closes it after the test. */
  def coilixContext(
    setup:   CoilixTestSetup,
    factory: TestContextFactory = TestContextFactory.default
  ): FunFixture[ConfigurableApplicationContext] =
    FunFixture[ConfigurableApplicationContext](_ => factory.create(setup), _.close())

  /** A fixture that builds one context before the first test of the suite and closes it after the last one. */
  def coilixSuiteContext(setup: CoilixTestSetup, factory: TestContextFactory = TestContextFactory.default): SuiteContextFixture =
    SuiteContextFixture(setup, factory)
}
