package dev.coilix.catseffect

import cats.effect.Async
import cats.effect.IO
import cats.effect.Sync
import cats.effect.Temporal
import cats.effect.unsafe.IORuntime
import dev.coilix.catseffect.tagless.PriceList
import dev.coilix.catseffect.tagless.QuoteService
import dev.coilix.testkit.CoilixContextOps
import dev.coilix.testkit.CoilixTestSetup
import munit.FunSuite

final class TaglessComponentsTest extends FunSuite with CoilixContextOps {
  private val shop = CoilixTestSetup.forPackages("dev.coilix.catseffect.tagless")

  test("creates tagless final components with F bound to IO and runs them on the IO runtime") {
    shop.use {
      val quote = bean[QuoteService[IO]].quote("brake pads").unsafeRunSync()(using bean[IORuntime])
      assertEquals(quote, "brake pads: 120")
      assertEquals(bean[PriceList[IO]].price("oil").unsafeRunSync()(using bean[IORuntime]), Some(45))
    }
  }

  test("the context bounds of the components receive the Async instance of IO") {
    shop.use {
      val async = bean[Async[IO]]
      assertEquals((bean[Sync[IO]] eq async, bean[Temporal[IO]] eq async, async eq IO.asyncForIO), (true, true, true))
    }
  }
}
