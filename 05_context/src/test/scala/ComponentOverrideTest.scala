package org.scaling.context

import munit.FunSuite
import org.scaling.config.ComponentProvider
import org.scaling.config.ConfigurationException
import org.scaling.context.overrides.PrefixedBeanNameGenerator
import org.scaling.context.wiring.EnglishGreeter

final class ComponentOverrideTest extends FunSuite {

  test("uses the built-in component when nothing is configured") {
    assertEquals(BeanNameGenerator.default(using ComponentProvider.builtIn).getClass, classOf[SimpleBeanNameGenerator])
  }

  test("replaces a single internal component with the configured class") {
    given ComponentProvider = Providers.fromHocon(
      """scaling.context.bean-name-generator {
        |  class = org.scaling.context.overrides.PrefixedBeanNameGenerator
        |  prefix = "my-"
        |  delegate.class = org.scaling.context.SimpleBeanNameGenerator
        |}""".stripMargin
    )
    assertEquals(BeanDefinitionFactory.default.create(classOf[EnglishGreeter]).map(_.beanName), List("my-englishGreeter"))
  }

  test("builds the configured class with its constructor parameters") {
    given ComponentProvider = Providers.fromHocon(
      "scaling.context.bean-name-generator { class = org.scaling.context.overrides.PrefixedBeanNameGenerator, prefix = x, delegate.class = org.scaling.context.SimpleBeanNameGenerator }"
    )
    assertEquals(BeanNameGenerator.default.getClass, classOf[PrefixedBeanNameGenerator])
  }

  test("fails when a constructor parameter of the configured class is missing") {
    given ComponentProvider = Providers.fromHocon(
      "scaling.context.bean-name-generator { class = org.scaling.context.overrides.PrefixedBeanNameGenerator, prefix = x }"
    )
    val error               = intercept[ConfigurationException](BeanNameGenerator.default)
    assert(error.getMessage.contains("scaling.context.bean-name-generator.delegate"))
  }

  test("fails when the configured class cannot be loaded") {
    given ComponentProvider = Providers.fromHocon("scaling.context.constructor-selector.class = com.example.Missing")
    intercept[ConfigurationException](BeanDefinitionFactory.default)
  }
}
