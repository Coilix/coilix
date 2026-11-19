package dev.coilix.boot

import dev.coilix.common.GenericType
import dev.coilix.common.TypeInfo
import dev.coilix.context.BeanDefinition

/**
 * Types whose discovered beans are left out of the context because the caller supplies its own instances, as a test does when it
 * replaces a bean with a stub.
 *
 * A discovered bean is replaced when its type, with its type arguments, can be assigned to one of the types, so replacing a trait also replaces every
 * implementation found by scanning, by `@Bean` methods or by modules.
 *
 * @param types
 *   the replaced types
 */
final case class BeanReplacements(types: List[GenericType]) {

  /** Whether `definition` is replaced by one of the types. */
  def replaces(definition: BeanDefinition): Boolean =
    types.exists(_.isAssignableFrom(definition.beanType))

  /** The types that replace none of `definitions`. */
  def unmatched(definitions: List[BeanDefinition]): List[GenericType] =
    types.filterNot(replacedType => definitions.exists(definition => replacedType.isAssignableFrom(definition.beanType)))

  /** These replacements and type `T`. */
  def and[T: TypeInfo]: BeanReplacements = BeanReplacements(types :+ TypeInfo[T].genericType)
}

/** Factories for [[dev.coilix.boot.BeanReplacements]]. */
object BeanReplacements {

  /** No replacements: every discovered bean is created. */
  val none: BeanReplacements = BeanReplacements(Nil)

  /** Replaces the discovered beans of type `T`. */
  def of[T: TypeInfo]: BeanReplacements = none.and[T]
}
