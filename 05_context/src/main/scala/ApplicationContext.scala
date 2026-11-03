package dev.coilix.context

import scala.reflect.ClassTag

/**
 * Read access to the beans of a running application.
 *
 * The context is itself a bean named `applicationContext`, so a component can inject it to look up beans that are not
 * known in advance. Prefer constructor parameters for dependencies known at compile time.
 *
 * ```scala
 * @Component
 * final class HandlerRegistry(context: ApplicationContext) {
 *   def handlers: List[Handler] = context.getBeansOfType[Handler]
 * }
 * ```
 */
trait ApplicationContext {

  /**
   * The bean of type `T`, chosen as described in [[dev.coilix.context.PrimaryCandidateSelector]].
   *
   * @throws dev.coilix.context.NoSuchBeanException
   *   if no bean has type `T`
   * @throws dev.coilix.context.AmbiguousBeanException
   *   if several beans have type `T` and none is marked with `@Primary`
   */
  def getBean[T: ClassTag]: T

  /**
   * The bean of type `T`, or `None` when there is none.
   *
   * @throws dev.coilix.context.AmbiguousBeanException
   *   if several beans have type `T` and none is marked with `@Primary`
   */
  def getBeanOption[T: ClassTag]: Option[T]

  /** All beans of type `T` in creation order, or an empty list. */
  def getBeansOfType[T: ClassTag]: List[T]

  /** The bean named `beanName`, or `None` when there is none. */
  def getBeanByName(beanName: String): Option[AnyRef]

  /** Names of all beans in creation order, including `applicationContext` and beans registered by the framework. */
  def beanNames: List[String]
}
