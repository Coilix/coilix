package dev.coilix.boot

import dev.coilix.boot.fixture.Clock
import dev.coilix.boot.fixture.FixedClock
import dev.coilix.config.ComponentProvider
import dev.coilix.context.BeanDefinitionFactory
import dev.coilix.context.RegisteredBean
import munit.FunSuite

final class DefaultCoilixBootstrapTest extends FunSuite {
  private given ComponentProvider = ComponentProvider.builtIn
  private val bootstrap           = CoilixBootstrap.default

  test("builds a context from the scanned package") {
    assertEquals(bootstrap.start(List("dev.coilix.boot.fixture"), Nil, Nil).getBean[Clock].now, 42L)
  }

  test("registers the context itself") {
    assert(bootstrap.start(List("dev.coilix.boot.fixture"), Nil, Nil).beanNames.contains("applicationContext"))
  }

  test("adds extra definitions to the scanned ones") {
    val extra = BeanDefinitionFactory.default.create(classOf[FixedClock])
    assertEquals(bootstrap.start(List("dev.coilix.boot.empty"), extra, Nil).getBean[Clock].now, 42L)
  }

  test("scans each package once") {
    val context = bootstrap.start(List("dev.coilix.boot.fixture", "dev.coilix.boot.fixture"), Nil, Nil)
    assertEquals(context.getBeansOfType[Clock].size, 1)
  }

  test("registers ready singletons") {
    val singleton = RegisteredBean("clock", classOf[FixedClock], isPrimary = false, FixedClock())
    assertEquals(bootstrap.start(List("dev.coilix.boot.empty"), Nil, List(singleton)).getBean[Clock].now, 42L)
  }
}
