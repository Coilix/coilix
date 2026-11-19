package dev.coilix.context

import dev.coilix.common.GenericType

/**
 * An immutable collection of created beans.
 *
 * [[dev.coilix.context.BeanRegistry.register]] returns a new registry, so a registry seen by one bean never changes later.
 */
trait BeanRegistry {

  /** All beans in registration order. */
  def beans: List[RegisteredBean]

  /** A registry with `bean` added after the existing beans. */
  def register(bean: RegisteredBean): BeanRegistry

  /**
   * The bean chosen for `beanType` by the [[dev.coilix.context.CandidateSelector]], or `None` when no bean matches.
   *
   * @throws dev.coilix.context.AmbiguousBeanException
   *   if several beans match and the selector cannot choose one
   */
  def find(beanType: GenericType): Option[RegisteredBean]

  /** All beans assignable to `beanType`, in registration order. */
  def findAll(beanType: GenericType): List[RegisteredBean]

  /** The bean named `beanName`, or `None` when there is none. */
  def findByName(beanName: String): Option[RegisteredBean]
}
