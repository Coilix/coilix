package dev.coilix.boot

import dev.coilix.boot.collected.JobLog
import dev.coilix.boot.collected.NightlyJob
import dev.coilix.boot.dependson.Ticker
import dev.coilix.boot.dependson.TickerAudit
import dev.coilix.boot.fixture.Clock
import dev.coilix.boot.fixture.FixedClock
import dev.coilix.boot.fixture.StubClock
import dev.coilix.boot.jobs.JobRunner
import dev.coilix.boot.jobs.JobsModule
import dev.coilix.boot.jobs.ModuleJob
import dev.coilix.boot.modules.EffectModule
import dev.coilix.boot.modules.FixedModuleLoader
import dev.coilix.boot.modules.RenamingModule
import dev.coilix.boot.modules.ReportModule
import dev.coilix.boot.modules.ReportTitle
import dev.coilix.boot.twins.Mirror
import dev.coilix.common.GenericType
import dev.coilix.config.ComponentProvider
import dev.coilix.context.ApplicationContextBuilder
import dev.coilix.context.BeanDefinition
import dev.coilix.context.BeanDefinitionFactory
import dev.coilix.context.ClassPathScanner
import dev.coilix.context.CoilixModule
import dev.coilix.context.CollectedComponents
import dev.coilix.context.ComponentCollector
import dev.coilix.context.ConfigurableApplicationContext
import dev.coilix.context.RegisteredBean
import dev.coilix.context.SynchronousLifecycle
import munit.FunSuite

final class DefaultCoilixBootstrapTest extends FunSuite {
  private given ComponentProvider = ComponentProvider.builtIn
  private val bootstrap           = CoilixBootstrap.default

  test("builds a context from the scanned package") {
    assertEquals(bootstrap.opened(List("dev.coilix.boot.fixture"), Nil, Nil).getBean[Clock].now, 42L)
  }

  test("registers the context itself") {
    assert(bootstrap.opened(List("dev.coilix.boot.fixture"), Nil, Nil).beanNames.contains("applicationContext"))
  }

  test("adds extra definitions to the scanned ones") {
    val extra = BeanDefinitionFactory.default.create(classOf[FixedClock])
    assertEquals(bootstrap.opened(List("dev.coilix.boot.empty"), extra, Nil).getBean[Clock].now, 42L)
  }

  test("scans each package once") {
    val context = bootstrap.opened(List("dev.coilix.boot.fixture", "dev.coilix.boot.fixture"), Nil, Nil)
    assertEquals(context.getBeansOfType[Clock].size, 1)
  }

  test("keeps every @Bean method of a class that returns the same type") {
    val context = bootstrap.opened(List("dev.coilix.boot.twins", "dev.coilix.boot.twins"), Nil, Nil)
    assertEquals(context.getBeansOfType[Mirror].map(_.side), List("left", "right"))
  }

  test("registers ready singletons") {
    val singleton = RegisteredBean("clock", GenericType.of(classOf[FixedClock]), isPrimary = false, FixedClock())
    assertEquals(bootstrap.opened(List("dev.coilix.boot.empty"), Nil, List(singleton)).getBean[Clock].now, 42L)
  }

  test("starts without any module") {
    assertEquals(withModules(Nil).opened(List("dev.coilix.boot.fixture"), Nil, Nil).getBean[Clock].now, 42L)
  }

  test("adds the components of the loaded modules, with their bean methods") {
    val context = withModules(List(ReportModule())).opened(List("dev.coilix.boot.empty"), Nil, Nil)
    assertEquals(context.getBean[ReportTitle], ReportTitle("Monthly report"))
  }

  test("leaves out the replaced beans so a ready instance takes their place") {
    val stub    = RegisteredBean("clock", GenericType.of(classOf[Clock]), isPrimary = false, StubClock(7L))
    val context = bootstrap.opened(List("dev.coilix.boot.fixture"), Nil, List(stub), BeanReplacements.of[Clock])
    assertEquals(context.getBeansOfType[Clock].map(_.now), List(7L))
  }

  test("replaces beans added by modules") {
    val title   = RegisteredBean("reportTitle", GenericType.of(classOf[ReportTitle]), isPrimary = false, ReportTitle("Stub report"))
    val context =
      withModules(List(ReportModule())).opened(List("dev.coilix.boot.empty"), Nil, List(title), BeanReplacements.of[ReportTitle])
    assertEquals(context.getBean[ReportTitle], ReportTitle("Stub report"))
  }

  test("keeps extra definitions of a replaced type") {
    val extra   = BeanDefinitionFactory.default.create(classOf[FixedClock])
    val context = bootstrap.opened(List("dev.coilix.boot.fixture"), extra, Nil, BeanReplacements.of[Clock])
    assertEquals(context.getBean[Clock].now, 42L)
  }

  test("a bean that depends on a replaced bean waits for nothing, because the replacement is ready") {
    val stub    = RegisteredBean("stubTicker", GenericType.of(classOf[Ticker]), isPrimary = false, Ticker())
    val context = bootstrap.opened(List("dev.coilix.boot.dependson"), Nil, List(stub), BeanReplacements.of[Ticker])
    assertEquals(context.getBeanOption[TickerAudit].isDefined, true)
  }

  test("does not register the collected components when no module collects annotations") {
    assert(!withModules(List(ReportModule())).opened(
      List("dev.coilix.boot.collected"),
      Nil,
      Nil
    ).beanNames.contains("collectedComponents"))
  }

  test("collects the annotated classes from scanned packages and module components instead of registering them") {
    val context = withModules(List(JobsModule())).opened(List("dev.coilix.boot.collected"), Nil, Nil)
    assertEquals(
      (context.getBean[CollectedComponents].classes, context.getBeanOption[NightlyJob], context.getBeanOption[ModuleJob]),
      (List(classOf[NightlyJob], classOf[ModuleJob]).sortBy(_.getName), None, None)
    )
  }

  test("lets a module bean create the collected classes with their dependencies") {
    val context = withModules(List(JobsModule())).opened(List("dev.coilix.boot.collected"), Nil, Nil)
    assertEquals((context.getBean[JobRunner].names, context.getBeansOfType[JobLog].size), (List("job nightly", "module"), 1))
  }

  test("a module collects no annotations by default") {
    assertEquals(ReportModule().collects, Nil)
  }

  test("applies the definition processors of modules to the beans of every module") {
    val context = withModules(List(ReportModule(), RenamingModule())).opened(List("dev.coilix.boot.empty"), Nil, Nil)
    assertEquals(
      (context.beanNames.contains("monthlyTitle"), context.beanNames.contains("reportTitle")),
      (true, false)
    )
  }

  test("a module with an effect interpreter does not change a context without effectful beans") {
    assertEquals(withModules(List(EffectModule("io"))).opened(List("dev.coilix.boot.fixture"), Nil, Nil).getBean[Clock].now, 42L)
  }

  test("a module has no effect interpreter by default") {
    assertEquals(ReportModule().effectInterpreter, None)
  }

  test("a module has no definition processors by default") {
    assertEquals(ReportModule().definitionProcessors, Nil)
  }

  private def withModules(modules: List[CoilixModule]): CoilixBootstrap =
    DefaultCoilixBootstrap(
      ClassPathScanner.default,
      ApplicationContextBuilder.default,
      FixedModuleLoader(modules),
      BeanDefinitionFactory.default,
      ComponentCollector.default
    )

  extension (bootstrap: CoilixBootstrap) {
    private def opened(
      basePackages: List[String],
      extra:        List[BeanDefinition],
      singletons:   List[RegisteredBean],
      replacements: BeanReplacements = BeanReplacements.none
    ): ConfigurableApplicationContext =
      SynchronousLifecycle.opened(StartupError.raised(bootstrap.start(basePackages, extra, singletons, replacements)))
  }
}
