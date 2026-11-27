package dev.coilix.context

import dev.coilix.common.GenericType

/**
 * Recognizes effects by the class name of the result type, without loading the effect library.
 *
 * Each map gives the position of the type argument that holds the value: `0` for `IO[A]` and `1` for `Resource[F, A]`. A result
 * type without that argument gives a wildcard value type.
 *
 * @param deferred
 *   class names of effects that produce the instance when they run
 * @param managed
 *   class names of resources that open the instance and release it later
 */
case class ClassNameEffectfulResultRecognizer(deferred: Map[String, Int], managed: Map[String, Int])
    extends EffectfulResultRecognizer {

  override def recognize(resultType: GenericType): Option[BeanResult] = {
    val className = resultType.erasure.getName
    deferred
      .get(className)
      .map(index => BeanResult.Deferred(resultType, valueAt(resultType, index)))
      .orElse(managed.get(className).map(index => BeanResult.Managed(resultType, valueAt(resultType, index))))
  }

  private def valueAt(resultType: GenericType, index: Int): GenericType =
    resultType match {
      case GenericType.Concrete(_, arguments) => arguments.lift(index).getOrElse(GenericType.Wildcard)
      case _                                  => GenericType.Wildcard
    }
}

/** The effects known to Coilix. */
object ClassNameEffectfulResultRecognizer {

  /**
   * Recognizes `cats.effect.IO[A]` as deferred and `cats.effect.kernel.Resource[F, A]` as managed, so a context without an
   * effect module reports such beans instead of registering the effect as the bean. This is the default
   * [[dev.coilix.context.EffectfulResultRecognizer]].
   */
  val knownEffects: ClassNameEffectfulResultRecognizer =
    ClassNameEffectfulResultRecognizer(
      deferred = Map("cats.effect.IO" -> 0),
      managed  = Map("cats.effect.kernel.Resource" -> 1)
    )
}
