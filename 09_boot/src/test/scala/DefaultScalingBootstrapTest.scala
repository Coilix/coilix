package org.scaling.boot

import munit.FunSuite
import org.scaling.boot.fixture.Clock
import org.scaling.boot.fixture.FixedClock
import org.scaling.config.ComponentProvider
import org.scaling.context.BeanDefinitionFactory
import org.scaling.context.RegisteredBean

final class DefaultScalingBootstrapTest extends FunSuite {
  private given ComponentProvider = ComponentProvider.builtIn
  private val bootstrap           = ScalingBootstrap.default

  test("builds a context from the scanned package") {
    assertEquals(bootstrap.start(List("org.scaling.boot.fixture"), Nil, Nil).getBean[Clock].now, 42L)
  }

  test("registers the context itself") {
    assert(bootstrap.start(List("org.scaling.boot.fixture"), Nil, Nil).beanNames.contains("applicationContext"))
  }

  test("adds extra definitions to the scanned ones") {
    val extra = BeanDefinitionFactory.default.create(classOf[FixedClock])
    assertEquals(bootstrap.start(List("org.scaling.boot.empty"), extra, Nil).getBean[Clock].now, 42L)
  }

  test("scans each package once") {
    val context = bootstrap.start(List("org.scaling.boot.fixture", "org.scaling.boot.fixture"), Nil, Nil)
    assertEquals(context.getBeansOfType[Clock].size, 1)
  }

  test("registers ready singletons") {
    val singleton = RegisteredBean("clock", classOf[FixedClock], isPrimary = false, FixedClock())
    assertEquals(bootstrap.start(List("org.scaling.boot.empty"), Nil, List(singleton)).getBean[Clock].now, 42L)
  }
}
