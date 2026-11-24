package dev.coilix.catseffect

import dev.coilix.boot.ApplicationArguments
import dev.coilix.boot.OrderedRunnerInvoker
import dev.coilix.catseffect.failing.FailureJournal
import dev.coilix.catseffect.parallel.Runner
import dev.coilix.catseffect.shop.OpeningJournal
import dev.coilix.catseffect.shop.Storefront
import dev.coilix.catseffect.slow.DrainConfiguration
import dev.coilix.catseffect.warmup.WarmupJournal
import dev.coilix.context.BeanCreationException
import dev.coilix.context.ConfigurableApplicationContext
import dev.coilix.testkit.CoilixContextOps
import dev.coilix.testkit.CoilixTestSetup
import munit.FunSuite

final class EffectfulBeansTest extends FunSuite with CoilixContextOps {

  private def setup(packageName: String): CoilixTestSetup =
    CoilixTestSetup.forPackages(s"dev.coilix.catseffect.$packageName").withProperties("coilix.concurrency.compute.threads" -> "2")

  test("gives other beans the values of IO and Resource beans") {
    setup("shop").use {
      val storefront = bean[Storefront]
      assertEquals((storefront.catalog.pool.name, storefront.greeting.text), ("main", "Welcome"))
    }
  }

  test("opens resources after the beans they need and closes them in reverse order") {
    val journal = setup("shop").use(bean[OpeningJournal])
    val entries = journal.all
    assertEquals(
      (entries.take(3).toSet, entries.drop(3)),
      (
        Set("pool opened", "mailbox opened", "greeting loaded"),
        List("catalog opened", "storefront created", "storefront closed", "catalog closed") ++ entries.takeRight(2)
      )
    )
    assertEquals(entries.takeRight(2).toSet, Set("pool closed", "mailbox closed"))
  }

  test("creates independent effectful beans at the same time") {
    setup("parallel").use {
      assertEquals(beans[Runner].map(_.name).toSet, Set("left", "right"))
    }
  }

  test("closes the opened resources when a bean fails to open") {
    val journal = FailureJournal()
    val failure = intercept[IllegalStateException](setup("failing").addBean(journal).use(()))
    assertEquals((failure.getMessage, journal.all), ("the forecast service is down", List("ledger opened", "ledger closed")))
  }

  test("goes on closing after a finalizer that fails or takes longer than its limit") {
    val started    = System.nanoTime
    val drains     = setup("slow").use(bean[DrainConfiguration])
    val closedInMs = (System.nanoTime - started) / 1000000
    assert(drains.isQuickDrainClosed)
    assert(closedInMs < 20000, s"closing took $closedInMs ms")
  }

  test("a lazy bean made by an effect stops the startup") {
    val failure = intercept[InvalidEffectfulBeanException](setup("lazyeffect").use(()))
    assert(failure.getMessage.contains("'snapshot'"), failure.getMessage)
  }

  test("runs @PostConstruct and @PreDestroy methods that return IO after and before the plain ones") {
    val journal = setup("warmup").use(bean[WarmupJournal])
    assertEquals(
      journal.all.filter(_.startsWith("cache")),
      List("cache loaded", "cache warmed", "cache flushed", "cache stopped")
    )
  }

  test("goes on closing after a @PreDestroy method that returns a failing IO") {
    val journal = setup("warmup").use(bean[WarmupJournal])
    assert(journal.all.contains("thermostat released"), journal.all.toString)
  }

  test("a @PostConstruct method that returns a failing IO stops the startup") {
    val failure = intercept[BeanCreationException](setup("brokenwarmup").use(()))
    assertEquals(
      (failure.getMessage.startsWith("@PostConstruct method heat of bean 'coldOven' failed"), failure.getCause.getMessage),
      (true, "the oven stays cold")
    )
  }

  test("runs IO runners and plain runners in one list by their order") {
    val journal = setup("warmup").use {
      OrderedRunnerInvoker().invoke(summon[ConfigurableApplicationContext], ApplicationArguments(Nil, Map.empty, List("today")))
      bean[WarmupJournal]
    }
    assertEquals(journal.all.filterNot(_.startsWith("cache")).take(3), List("prices for today", "report", "audit"))
  }
}
