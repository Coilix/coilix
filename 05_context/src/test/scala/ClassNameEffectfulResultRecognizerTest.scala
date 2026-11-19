package dev.coilix.context

import dev.coilix.common.GenericType
import dev.coilix.context.effects.Archive
import dev.coilix.context.effects.Later
import dev.coilix.context.effects.Report
import dev.coilix.context.effects.Scoped
import dev.coilix.context.effects.TestEffects
import munit.FunSuite

final class ClassNameEffectfulResultRecognizerTest extends FunSuite {
  private val report  = GenericType.of(classOf[Report])
  private val archive = GenericType.of(classOf[Archive])
  private val later   = GenericType.Concrete(classOf[Later[?]], List(report))
  private val scoped  = GenericType.Concrete(classOf[Scoped[?, ?]], List(GenericType.of(classOf[Option[?]]), archive))

  test("recognizes a deferred effect with the value at its argument") {
    assertEquals(TestEffects.recognizer.recognize(later), Some(BeanResult.Deferred(later, report)))
  }

  test("recognizes a managed resource with the value at its argument") {
    assertEquals(TestEffects.recognizer.recognize(scoped), Some(BeanResult.Managed(scoped, archive)))
  }

  test("leaves other types plain") {
    assertEquals(TestEffects.recognizer.recognize(report), None)
  }

  test("gives a wildcard value when the effect has no type arguments") {
    val raw = GenericType.of(classOf[Later[?]])
    assertEquals(TestEffects.recognizer.recognize(raw), Some(BeanResult.Deferred(raw, GenericType.Wildcard)))
  }

  test("known effects leave types outside cats-effect plain") {
    assertEquals(ClassNameEffectfulResultRecognizer.knownEffects.recognize(later), None)
  }
}
