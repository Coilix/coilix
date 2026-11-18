package dev.coilix.context

import dev.coilix.config.ComponentProvider
import dev.coilix.context.conditions.AuditLog
import dev.coilix.context.conditions.Clock
import dev.coilix.context.conditions.ClockDefaults
import dev.coilix.context.conditions.DevGreeter
import dev.coilix.context.conditions.DevLabels
import dev.coilix.context.conditions.FixedClock
import dev.coilix.context.conditions.GreetingService
import dev.coilix.context.conditions.Label
import dev.coilix.context.conditions.MissingDriverSupport
import dev.coilix.context.conditions.PresentDriverSupport
import dev.coilix.context.conditions.ProdGreeter
import dev.coilix.context.conditions.SystemClock
import munit.FunSuite

final class ConditionalBeansTest extends FunSuite {

  private def contextOf(hocon: String, classes: Class[?]*): ApplicationContext = {
    given ComponentProvider = Providers.fromHocon(hocon)
    ApplicationContextBuilder.default.build(classes.toList.flatMap(BeanDefinitionFactory.default.create))
  }

  private val greeters = List(classOf[DevGreeter], classOf[ProdGreeter], classOf[GreetingService])

  test("a profile bean is added only when its profile is active, without ambiguity") {
    assertEquals(contextOf("coilix.profiles.active = [dev]", greeters*).getBean[GreetingService].greeter.greet, "dev")
    assertEquals(contextOf("", greeters*).getBean[GreetingService].greeter.greet, "prod")
  }

  test("the profile of a configuration class applies to its bean methods") {
    assertEquals(contextOf("coilix.profiles.active = dev", classOf[DevLabels]).getBean[Label], Label("dev"))
    assertEquals(contextOf("", classOf[DevLabels]).getBeansOfType[Label], Nil)
  }

  test("a property condition compares the configured value") {
    assertEquals(contextOf("shop.audit.enabled = true", classOf[AuditLog]).getBeansOfType[AuditLog].size, 1)
    assertEquals(contextOf("shop.audit.enabled = false", classOf[AuditLog]).getBeansOfType[AuditLog], Nil)
    assertEquals(contextOf("", classOf[AuditLog]).getBeansOfType[AuditLog], Nil)
  }

  test("a class condition checks the classpath") {
    val context = contextOf("", classOf[MissingDriverSupport], classOf[PresentDriverSupport])
    assertEquals(context.getBeansOfType[MissingDriverSupport], Nil)
    assertEquals(context.getBeansOfType[PresentDriverSupport].size, 1)
  }

  test("a bean marked as missing-only is used when nothing else provides the type") {
    assertEquals(contextOf("", classOf[ClockDefaults]).getBean[Clock].getClass, classOf[SystemClock])
  }

  test("a bean marked as missing-only gives way to an application bean of the same type") {
    val context = contextOf("", classOf[ClockDefaults], classOf[FixedClock])
    assertEquals(context.getBean[Clock].now, 42L)
    assertEquals(context.getBeansOfType[Clock].size, 1)
  }

  test("a bean marked as missing-only gives way to a ready singleton") {
    given ComponentProvider = ComponentProvider.builtIn
    val singleton           = RegisteredBean("clock", classOf[FixedClock], isPrimary = false, FixedClock())
    val context             =
      ApplicationContextBuilder.default.build(BeanDefinitionFactory.default.create(classOf[ClockDefaults]), List(singleton))
    assertEquals(context.getBeansOfType[Clock].size, 1)
  }
}
