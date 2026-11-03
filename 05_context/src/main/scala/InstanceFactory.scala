package dev.coilix.context

/** Creates the instance of a bean from its resolved dependencies. */
trait InstanceFactory {

  /**
   * Creates the instance.
   *
   * @param arguments
   *   values of the bean's dependencies, in the order of [[dev.coilix.context.BeanDefinition.dependencies]]
   */
  def create(arguments: List[AnyRef]): AnyRef
}
