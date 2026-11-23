package dev.coilix.context

import dev.coilix.common.GenericType
import dev.coilix.config.ComponentProvider
import dev.coilix.context.effects.Later
import dev.coilix.context.effects.LaterInterpreter
import dev.coilix.context.effects.ReportReader
import dev.coilix.context.effects.TestEffects
import dev.coilix.context.ordering.AlphaService
import dev.coilix.context.ordering.ItemCatalog
import dev.coilix.context.ordering.YankeeItem
import dev.coilix.context.ordering.ZuluAudit
import dev.coilix.context.ordering.ZuluItem
import dev.coilix.context.wiring.ContextAwareBean
import dev.coilix.context.wiring.DefaultGreetingService
import dev.coilix.context.wiring.EnglishGreeter
import dev.coilix.context.wiring.GreetingService
import dev.coilix.context.wiring.PolishGreeter
import java.util.concurrent.atomic.AtomicReference
import munit.FunSuite

final class DefaultApplicationContextBuilderTest extends FunSuite {
  private given ComponentProvider = ComponentProvider.builtIn

  private val context = ApplicationContextBuilder.default.build(
    Definitions.of(classOf[ContextAwareBean], classOf[DefaultGreetingService], classOf[EnglishGreeter], classOf[PolishGreeter])
  )

  test("injects the primary bean") {
    assertEquals(context.getBean[GreetingService].greet("Ann"), "Cześć Ann")
  }

  test("injects all beans of a collection type") {
    assertEquals(context.getBean[GreetingService].greetAll("Ann").sorted, List("Cześć Ann", "Hello Ann"))
  }

  test("injects none for a missing optional bean") {
    assert(!context.getBean[GreetingService].hasFarewell)
  }

  test("injects the context itself") {
    assert(context.getBean[ContextAwareBean].context eq context)
  }

  test("injected context sees every bean") {
    assertEquals(
      context.getBean[ContextAwareBean].context.beanNames.sorted,
      List("applicationContext", "contextAwareBean", "defaultGreetingService", "englishGreeter", "polishGreeter")
    )
  }

  test("injects a ready singleton") {
    val singleton = RegisteredBean("englishGreeter", GenericType.of(classOf[EnglishGreeter]), isPrimary = false, EnglishGreeter())
    val built     = ApplicationContextBuilder.default.build(Definitions.of(classOf[DefaultGreetingService]), List(singleton))
    assertEquals(built.getBean[GreetingService].greet("Ann"), "Hello Ann")
  }

  test("builds an active context") {
    assert(context.isActive)
  }

  test("injects optional and collection dependencies defined after their dependant") {
    val built = ApplicationContextBuilder.default.build(
      Definitions.of(classOf[AlphaService], classOf[ItemCatalog], classOf[YankeeItem], classOf[ZuluAudit], classOf[ZuluItem])
    )
    assertEquals((built.getBean[AlphaService].audit.isDefined, built.getBean[ItemCatalog].items.size), (true, 2))
  }

  test("rejects beans created by effects with a hint about an effect module") {
    val definitions = TestEffects.definitions ++ Definitions.of(classOf[ReportReader])
    val error       = intercept[EffectfulBeanException](ApplicationContextBuilder.default.build(definitions))
    assertEquals(error.definitions.map(_.beanName), List("archive", "report"))
    assert(error.getMessage.contains(s"'report' (${classOf[Later[?]].getName}["), error.getMessage)
    assert(error.getMessage.contains("CoilixIOApp"), error.getMessage)
  }

  test("hands the levels from the first effectful bean to the interpreter and releases them when the context closes") {
    val interpreter = LaterInterpreter()
    val built       =
      ApplicationContextBuilder.default.build(
        TestEffects.definitions ++ Definitions.of(classOf[ReportReader]),
        Nil,
        Some(interpreter)
      )
    val title       = built.getBean[ReportReader].report.title
    built.close()
    assertEquals((title, interpreter.journal), ("daily", List("levels archive+report, reportReader", "released")))
  }

  test("does not use the interpreter without effectful beans") {
    val interpreter = LaterInterpreter()
    ApplicationContextBuilder.default.build(Definitions.of(classOf[EnglishGreeter]), Nil, Some(interpreter)).close()
    assertEquals(interpreter.journal, Nil)
  }

  test("destroys the beans created before the interpreter when it fails") {
    val destroyed = AtomicReference(List.empty[String])
    val builder   = DefaultApplicationContextBuilder(
      ContextPlanner.default,
      BeanInstantiator.default,
      CandidateSelector.default,
      beans => destroyed.set(beans.map(_.beanName))
    )
    intercept[IllegalStateException](builder.build(TestEffects.definitions, Nil, Some(LaterInterpreter(failing = true))))
    assertEquals(destroyed.get, List("applicationContext", "effectConfiguration"))
  }
}
