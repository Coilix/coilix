package dev.coilix.context

import dev.coilix.config.ComponentProvider
import dev.coilix.context.dependson.AuditTrail
import dev.coilix.context.dependson.ContextWatcher
import dev.coilix.context.dependson.DraftNote
import dev.coilix.context.dependson.GhostDependant
import dev.coilix.context.dependson.LedgerCache
import dev.coilix.context.dependson.NoteBoard
import dev.coilix.context.dependson.PriceCache
import dev.coilix.context.dependson.SchemaMigrations
import dev.coilix.context.dependson.SelfDependant
import dev.coilix.context.dependson.StartupJournal
import dev.coilix.context.dependson.WarmupConfiguration
import munit.FunSuite

final class DependsOnTest extends FunSuite {
  private given ComponentProvider = ComponentProvider.builtIn

  test("reads @DependsOn on a class and on a @Bean method, without repeated names") {
    val dependsOn = Definitions.of(classOf[PriceCache], classOf[WarmupConfiguration]).map(d => d.beanName -> d.dependsOn).toMap
    assertEquals(
      (dependsOn("priceCache"), dependsOn("cacheWarmer"), dependsOn("warmupConfiguration")),
      (List("schemaMigrations"), List("priceCache", "schemaMigrations"), Nil)
    )
  }

  test("creates the named beans first and destroys them last") {
    val context = contextOf(classOf[PriceCache], classOf[SchemaMigrations])
    context.close()
    assertEquals(
      context.getBean[StartupJournal].all,
      List("migrated", "prices loaded", "prices dropped", "migrations closed")
    )
  }

  test("orders a @Bean method after the beans it depends on") {
    val context = contextOf(classOf[WarmupConfiguration], classOf[PriceCache], classOf[SchemaMigrations])
    assertEquals(context.getBean[StartupJournal].all, List("migrated", "prices loaded", "cache warmed"))
  }

  test("a lazy bean creates the lazy beans it depends on when it is first needed") {
    val context = contextOf(classOf[LedgerCache], classOf[AuditTrail])
    val atStart = context.getBean[StartupJournal].all
    val _       = context.getBean[LedgerCache]
    assertEquals((atStart, context.getBean[StartupJournal].all), (Nil, List("audit opened", "ledger loaded")))
  }

  test("a name of a ready instance is already satisfied") {
    assertEquals(contextOf(classOf[ContextWatcher], classOf[SchemaMigrations]).getBeanOption[ContextWatcher].isDefined, true)
  }

  test("a name that matches no bean fails the startup") {
    val failure = intercept[NoSuchBeanException](contextOf(classOf[GhostDependant]))
    assertEquals(failure.getMessage, "No bean named 'ghost' that 'ghostDependant' depends on")
  }

  test("a bean that depends on itself is a circular dependency") {
    intercept[CircularDependencyException](contextOf(classOf[SelfDependant]))
  }

  test("depending on a prototype fails the startup") {
    val failure = intercept[BeanCreationException](contextOf(classOf[NoteBoard], classOf[DraftNote]))
    assert(failure.getMessage.contains("prototype 'draftNote'"), failure.getMessage)
  }

  private def contextOf(classes: Class[?]*): ConfigurableApplicationContext =
    ApplicationContextBuilder.default.build(Definitions.of((classOf[StartupJournal] +: classes)*))
}
