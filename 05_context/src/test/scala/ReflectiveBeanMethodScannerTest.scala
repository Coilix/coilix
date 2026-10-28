package org.scaling.context

import munit.FunSuite
import org.scaling.context.configured.Salutation
import org.scaling.context.configured.SalutationConfiguration
import org.scaling.context.configured.ServerSettings
import org.scaling.context.configured.Signature
import org.scaling.context.wiring.EnglishGreeter

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
