package dev.coilix.boot

import dev.coilix.common.ClassTags.runtimeTypeOf
import dev.coilix.context.BeanDefinition
import scala.reflect.ClassTag

/**
 * Types whose discovered beans are left out of the context because the caller supplies its own instances, as a test does when it
 * replaces a bean with a stub.
 *
 * A discovered bean is replaced when its class can be assigned to one of the types, so replacing a trait also replaces every
 * implementation found by scanning, by `@Bean` methods or by modules.
 *
 * @param types
 *   the replaced types
 */
final case class BeanReplacements(types: List[Class[?]]) {

  /** Whether `definition` is replaced by one of the types. */
  def replaces(definition: BeanDefinition): Boolean =
    types.exists(_.isAssignableFrom(definition.beanClass))

  /** The types that replace none of `definitions`. */
  def unmatched(definitions: List[BeanDefinition]): List[Class[?]] =
    types.filterNot(replacedType => definitions.exists(definition => replacedType.isAssignableFrom(definition.beanClass)))

  /** These replacements and type `T`. */
  def and[T: ClassTag]: BeanReplacements = BeanReplacements(types :+ runtimeTypeOf[T])
}

/** Factories for [[dev.coilix.boot.BeanReplacements]]. */
object BeanReplacements {

  /** No replacements: every discovered bean is created. */
  val none: BeanReplacements = BeanReplacements(Nil)

  /** Replaces the discovered beans of type `T`. */
  def of[T: ClassTag]: BeanReplacements = none.and[T]
}
