package dev.coilix.context

import dev.coilix.common.GenericType
import dev.coilix.config.ComponentProvider
import dev.coilix.context.qualified.Banner
import dev.coilix.context.qualified.BannerConfiguration
import dev.coilix.context.qualified.MissingQualifier
import dev.coilix.context.qualified.QualifiedGreeting
import dev.coilix.context.qualified.QualifiedOption
import dev.coilix.context.wiring.EnglishGreeter
import dev.coilix.context.wiring.Greeter
import dev.coilix.context.wiring.PolishGreeter
import munit.FunSuite

final class QualifierTest extends FunSuite {
  private given ComponentProvider = ComponentProvider.builtIn

  test("maps a qualified parameter to a named dependency") {
    assertEquals(
      Definitions.of(classOf[QualifiedGreeting]).map(_.dependencies),
      List(List(Dependency.Named("englishGreeter", GenericType.of(classOf[Greeter]))))
    )
  }

  test("injects the named bean instead of the primary one") {
    val context = ApplicationContextBuilder.default.build(
      Definitions.of(classOf[QualifiedGreeting], classOf[PolishGreeter], classOf[EnglishGreeter])
    )
    assertEquals(context.getBean[QualifiedGreeting].greeter.greet("Ann"), "Hello Ann")
  }

  test("injects the named bean into a @Bean method") {
    val context = ApplicationContextBuilder.default.build(
      Definitions.of(classOf[BannerConfiguration], classOf[PolishGreeter], classOf[EnglishGreeter])
    )
    assertEquals(context.getBean[Banner], Banner("Hello guests"))
  }

  test("injects a named ready singleton") {
    val singleton = RegisteredBean.of[Greeter]("englishGreeter", EnglishGreeter())
    val context   = ApplicationContextBuilder.default.build(Definitions.of(classOf[QualifiedGreeting]), List(singleton))
    assertEquals(context.getBean[QualifiedGreeting].greeter.greet("Ann"), "Hello Ann")
  }

  test("fails when no bean has the name") {
    val error = intercept[NoSuchBeanException] {
      ApplicationContextBuilder.default.build(Definitions.of(classOf[MissingQualifier], classOf[EnglishGreeter]))
    }
    assert(error.getMessage.contains("No bean named 'frenchGreeter'"), error.getMessage)
  }

  test("rejects @Qualifier on an optional dependency") {
    val error = intercept[UnsupportedDependencyTypeException](Definitions.of(classOf[QualifiedOption]))
    assert(error.getMessage.contains("has @Qualifier, which selects one bean"), error.getMessage)
  }
}
