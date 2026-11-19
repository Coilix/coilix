package dev.coilix.context

import dev.coilix.common.GenericType
import dev.coilix.config.ComponentProvider
import dev.coilix.config.ConfigPath

/** Recognizes result types of bean factories that are effects, such as `IO[A]` or `Resource[IO, A]`. */
trait EffectfulResultRecognizer {

  /**
   * The kind of result for the declared type, or `None` when the type is not an effect and the bean is the returned value.
   *
   * @param resultType
   *   the declared result type of a bean factory, such as the return type of a `@Bean` method
   */
  def recognize(resultType: GenericType): Option[BeanResult]
}

/** Configuration path and default implementation of [[dev.coilix.context.EffectfulResultRecognizer]]. */
object EffectfulResultRecognizer {

  /**
   * Replace the component in the configuration under `coilix.context.effectful-result-recognizer`, with a `class` key naming
   * the implementation.
   */
  given ConfigPath[EffectfulResultRecognizer] = ConfigPath[EffectfulResultRecognizer]("coilix.context.effectful-result-recognizer")

  /**
   * The configured implementation, or [[dev.coilix.context.ClassNameEffectfulResultRecognizer.knownEffects]] when
   * `coilix.context.effectful-result-recognizer` is not set.
   */
  def default(using components: ComponentProvider): EffectfulResultRecognizer =
    components.get[EffectfulResultRecognizer](ClassNameEffectfulResultRecognizer.knownEffects)
}
