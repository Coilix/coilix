package dev.coilix.context

import dev.coilix.common.GenericType

/**
 * An immutable collection of beans: created ones and [[dev.coilix.context.DeferredBean]]s, whose instances are created when
 * they are looked up.
 *
 * [[dev.coilix.context.BeanRegistry.register]] and [[dev.coilix.context.BeanRegistry.defer]] return a new registry, so a
 * registry seen by one bean never gains beans later. Looking up a deferred bean creates its instance: once for a lazy singleton,
 * every time for a prototype.
 */
trait BeanRegistry {

  /** The beans that have an instance, in registration order: every eager bean and the lazy beans created so far. */
  def beans: List[RegisteredBean]

  /** The names of all beans, also of the deferred ones, in registration order. */
  def beanNames: List[String]

  /** A registry with `bean` added after the existing beans. */
  def register(bean: RegisteredBean): BeanRegistry

  /** A registry with the deferred `bean` added after the existing beans. */
  def defer(bean: DeferredBean): BeanRegistry

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

  /**
   * The registry of the started context, with every bean, or this registry while the context is still starting. A
   * [[dev.coilix.context.Provider]] looks beans up here.
   */
  def complete: BeanRegistry = this
}
