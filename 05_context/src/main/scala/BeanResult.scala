package dev.coilix.context

import dev.coilix.common.GenericType

/**
 * What the [[dev.coilix.context.InstanceFactory]] of a bean returns: the instance itself, or an effect that gives it.
 *
 * Effectful results are found by an [[dev.coilix.context.EffectfulResultRecognizer]]. The bean is registered under the type of
 * the value inside the effect, so dependants receive `A`, not `IO[A]`. Only an interpreter that runs the effect, such as the one
 * in an effect module, can create such a bean. The synchronous [[dev.coilix.context.DefaultApplicationContextBuilder]] rejects
 * it.
 */
enum BeanResult {

  /** The factory returns the instance. */
  case Plain

  /**
   * The factory returns an effect that produces the instance when it runs, such as `IO[A]`.
   *
   * @param effectType
   *   the declared result type, such as `cats.effect.IO[Report]`
   * @param valueType
   *   the type of the produced instance, such as `Report`
   */
  case Deferred(effectType: GenericType, valueType: GenericType)

  /**
   * The factory returns a resource that opens the instance and releases it when the context closes, such as `Resource[IO, A]`.
   *
   * @param resourceType
   *   the declared result type, such as `cats.effect.kernel.Resource[cats.effect.IO, Transactor]`
   * @param valueType
   *   the type of the opened instance, such as `Transactor`
   */
  case Managed(resourceType: GenericType, valueType: GenericType)

  /** Whether the instance comes from an effect that has to run. */
  def isEffectful: Boolean = this != Plain

  /** The type under which the bean is registered: the value type of an effect, or `declared` for a plain result. */
  def beanType(declared: GenericType): GenericType =
    this match {
      case Plain                  => declared
      case Deferred(_, valueType) => valueType
      case Managed(_, valueType)  => valueType
    }

  /** The declared type of the effect, or `None` for a plain result. */
  def declaredType: Option[GenericType] =
    this match {
      case Plain                    => None
      case Deferred(effectType, _)  => Some(effectType)
      case Managed(resourceType, _) => Some(resourceType)
    }
}
