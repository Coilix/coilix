package dev.coilix.context

final class DefaultApplicationContextBuilder(
  resolver:     DependencyResolver,
  instantiator: BeanInstantiator,
  selector:     CandidateSelector
) extends ApplicationContextBuilder {

  private val contextBeanName = "applicationContext"

  override def build(definitions: List[BeanDefinition], singletons: List[RegisteredBean]): ConfigurableApplicationContext = {
    lazy val context:  ConfigurableApplicationContext = DefaultApplicationContext(registry)
    lazy val registry: BeanRegistry                   = instantiateAll(definitions, initialRegistry(context, singletons))
    val _ = registry
    context
  }

  private def initialRegistry(context: ApplicationContext, singletons: List[RegisteredBean]): BeanRegistry =
    singletons.foldLeft(contextRegistry(context))(_.register(_))

  private def contextRegistry(context: ApplicationContext): BeanRegistry =
    ImmutableBeanRegistry.empty(selector).register(RegisteredBean(contextBeanName, context.getClass, isPrimary = false, context))

  private def instantiateAll(definitions: List[BeanDefinition], initial: BeanRegistry): BeanRegistry =
    resolver
      .resolve(definitions, initial.beans.map(_.beanClass))
      .foldLeft(initial)(instantiate)

  private def instantiate(registry: BeanRegistry, definition: BeanDefinition): BeanRegistry =
    registry.register(instantiator.instantiate(definition, registry))
}
