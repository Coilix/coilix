package dev.coilix.context

import dev.coilix.common.GenericType
import dev.coilix.context.configured.Salutation
import dev.coilix.context.configured.SalutationConfiguration
import dev.coilix.context.configured.ServerSettings
import dev.coilix.context.configured.Signature
import dev.coilix.context.effects.Archive
import dev.coilix.context.effects.EffectConfiguration
import dev.coilix.context.effects.Later
import dev.coilix.context.effects.Report
import dev.coilix.context.effects.Scoped
import dev.coilix.context.effects.TestEffects
import dev.coilix.context.wiring.EnglishGreeter
import munit.FunSuite

final class ReflectiveBeanMethodScannerTest extends FunSuite {
  private val scanner = ReflectiveBeanMethodScanner(
    AnnotatedParameterDependencyMapper(GenericDependencyTypeMapper()),
    ClassNameEffectfulResultRecognizer.knownEffects
  )

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
        Dependency.Required(GenericType.of(classOf[SalutationConfiguration])),
        Dependency.Required(GenericType.of(classOf[Salutation])),
        Dependency.Required(GenericType.of(classOf[ServerSettings]))
      )
    )
  }

  test("registers a bean returned in an effect under the type of its value") {
    val report = GenericType.of(classOf[Report])
    assertEquals(
      TestEffects.scanner.scan(classOf[EffectConfiguration]).map(definition => (definition.beanType, definition.result)),
      List(
        GenericType.of(classOf[Archive]) -> BeanResult.Managed(
          GenericType.Concrete(
            classOf[Scoped[?, ?]],
            List(GenericType.of(classOf[Option[?]]), GenericType.of(classOf[Archive]))
          ),
          GenericType.of(classOf[Archive])
        ),
        report                           -> BeanResult.Deferred(GenericType.Concrete(classOf[Later[?]], List(report)), report)
      )
    )
  }

  test("ignores classes without @Configuration") {
    assertEquals(scanner.scan(classOf[EnglishGreeter]), Nil)
  }
}
