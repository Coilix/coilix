package dev.coilix.context

/**
 * Builds a [[dev.coilix.context.DefaultApplicationContext]]: registers the context itself as `applicationContext` and the given
 * singletons, leaves out the definitions rejected by the [[dev.coilix.context.DefinitionFilter]], orders the rest with the
 * [[dev.coilix.context.DependencyResolver]] and creates each bean with the
 * [[dev.coilix.context.BeanInstantiator]].
 *
 * This is the default [[dev.coilix.context.ApplicationContextBuilder]].
 *
 * @param resolver
 *   orders bean definitions by their dependencies
 * @param instantiator
 *   creates each bean
 * @param selector
 *   chooses a bean when several match a requested type
 * @param destroyer
 *   destroys the beans when the built context closes
 * @param filter
 *   leaves out definitions whose conditions do not hold
 */
final class DefaultApplicationContextBuilder(
  resolver:     DependencyResolver,
  instantiator: BeanInstantiator,
  selector:     CandidateSelector,
  destroyer:    BeanDestroyer,
  filter:       DefinitionFilter
) extends ApplicationContextBuilder {

  private val contextBeanName = "applicationContext"

  override def build(definitions: List[BeanDefinition], singletons: List[RegisteredBean]): ConfigurableApplicationContext = {
    lazy val context:  ConfigurableApplicationContext = DefaultApplicationContext(registry, destroyer)
    lazy val registry: BeanRegistry                   = instantiateAll(definitions, initialRegistry(context, singletons))
    val _ = registry
    context
  }

  private def initialRegistry(context: ApplicationContext, singletons: List[RegisteredBean]): BeanRegistry =
    singletons.foldLeft(contextRegistry(context))(_.register(_))

  private def contextRegistry(context: ApplicationContext): BeanRegistry =
    ImmutableBeanRegistry.empty(selector).register(RegisteredBean.ofClass(contextBeanName, context.getClass, context))

  private def instantiateAll(definitions: List[BeanDefinition], initial: BeanRegistry): BeanRegistry =
    resolver
      .resolve(filter.filter(definitions, initial.beans.map(_.beanClass)), initial.beans.map(_.beanType))
      .foldLeft(initial)(instantiate)

  private def instantiate(registry: BeanRegistry, definition: BeanDefinition): BeanRegistry =
    registry.register(instantiator.instantiate(definition, registry))
}
