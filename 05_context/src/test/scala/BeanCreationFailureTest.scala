package dev.coilix.context

import dev.coilix.config.ComponentProvider
import dev.coilix.context.failing.BrokenConfiguration
import dev.coilix.context.failing.BrokenLinkage
import dev.coilix.context.failing.BrokenScale
import munit.FunSuite

final class BeanCreationFailureTest extends FunSuite {
  private given ComponentProvider = ComponentProvider.builtIn

  test("reports an exception of a constructor as a BeanCreationException caused by it") {
    val error = intercept[BeanCreationException](ConstructorInstanceFactory(classOf[BrokenScale].getConstructors.head).create(Nil))
    assertEquals(
      error.getMessage,
      "Constructor of dev.coilix.context.failing.BrokenScale failed: java.lang.IllegalStateException: the scale is not calibrated"
    )
    assert(error.getCause.isInstanceOf[IllegalStateException])
  }

  test("reports an exception of a @Bean method as a BeanCreationException caused by it") {
    val factory = MethodInstanceFactory(classOf[BrokenConfiguration].getMethod("weight"))
    val error   = intercept[BeanCreationException](factory.create(List(BrokenConfiguration())))
    assertEquals(
      error.getMessage,
      "@Bean method dev.coilix.context.failing.BrokenConfiguration.weight failed: java.lang.IllegalArgumentException: no weight"
    )
    assert(error.getCause.isInstanceOf[IllegalArgumentException])
  }

  test("rethrows a fatal error of a constructor as it is") {
    val thrown =
      try {
        ConstructorInstanceFactory(classOf[BrokenLinkage].getConstructors.head).create(Nil)
        None
      } catch {
        case error: LinkageError => Option(error)
      }
    assertEquals(thrown.map(_.getMessage), Some("a library is missing"))
  }

  test("a context with a failing bean fails to start with a BeanCreationException") {
    val error = intercept[BeanCreationException](
      ApplicationContextBuilder.default.build(List(classOf[BrokenScale]).flatMap(BeanDefinitionFactory.default.create))
    )
    assert(error.getMessage.contains("the scale is not calibrated"), error.getMessage)
  }
}
