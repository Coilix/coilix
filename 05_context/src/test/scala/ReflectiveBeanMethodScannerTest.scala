package dev.coilix.context

import dev.coilix.context.configured.Salutation
import dev.coilix.context.configured.SalutationConfiguration
import dev.coilix.context.configured.ServerSettings
import dev.coilix.context.configured.Signature
import dev.coilix.context.wiring.EnglishGreeter
import munit.FunSuite

final class ReflectiveBeanMethodScannerTest extends FunSuite {
  private val scanner = ReflectiveBeanMethodScanner(AnnotatedParameterDependencyMapper(GenericDependencyTypeMapper()))

  test("describes each @Bean method of a configuration class") {
    assertEquals(
      scanner.scan(classOf[SalutationConfiguration]).map(definition => (definition.beanName, definition.beanClass)),
      List("salutation" -> classOf[Salutation], "companySignature" -> classOf[Signature])
    )
  }

  test("makes the configuration class the first dependency") {
    assertEquals(
      scanner.scan(classOf[SalutationConfiguration]).last.dependencies,
      List(
        Dependency.Required(classOf[SalutationConfiguration]),
        Dependency.Required(classOf[Salutation]),
        Dependency.Required(classOf[ServerSettings])
      )
    )
  }

  test("ignores classes without @Configuration") {
    assertEquals(scanner.scan(classOf[EnglishGreeter]), Nil)
  }
}
