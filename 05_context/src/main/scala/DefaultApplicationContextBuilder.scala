package dev.coilix.context

/**
 * Builds a [[dev.coilix.context.DefaultApplicationContext]] by running a [[dev.coilix.context.ContextPlan]] synchronously.
 *
 * The context itself is provided as `applicationContext`, before the given singletons. The plan from the
 * [[dev.coilix.context.ContextPlanner]] is checked before any bean is created: a bean whose factory returns an effect stops the
 * build with an [[dev.coilix.context.EffectfulBeanException]], because this builder does not run effects. Each other bean is
 * created with the [[dev.coilix.context.BeanInstantiator]].
 *
 * This is the default [[dev.coilix.context.ApplicationContextBuilder]].
 *
 * @param planner
 *   leaves out definitions whose conditions do not hold and orders the rest by their dependencies
 * @param instantiator
 *   creates each bean
 * @param selector
 *   chooses a bean when several match a requested type
 * @param destroyer
 *   destroys the beans when the built context closes
 */
final class DefaultApplicationContextBuilder(
  planner:      ContextPlanner,
  instantiator: BeanInstantiator,
  selector:     CandidateSelector,
  destroyer:    BeanDestroyer
) extends ApplicationContextBuilder {

  private val contextBeanName = "applicationContext"

  override def build(definitions: List[BeanDefinition], singletons: List[RegisteredBean]): ConfigurableApplicationContext = {
    lazy val context:  ConfigurableApplicationContext = DefaultApplicationContext(registry, destroyer)
    lazy val registry: BeanRegistry                   = execute(planner.plan(definitions, contextBean(context) :: singletons))
    val _ = registry
    context
  }

  private def contextBean(context: ApplicationContext): RegisteredBean =
    RegisteredBean.ofClass(contextBeanName, context.getClass, context)

  private def execute(plan: ContextPlan): BeanRegistry =
    if (plan.effectful.nonEmpty) throw EffectfulBeanException(plan.effectful)
    else plan.steps.foldLeft(ImmutableBeanRegistry.empty(selector))(execute)

  private def execute(registry: BeanRegistry, step: PlanStep): BeanRegistry =
    step match {
      case PlanStep.Provide(bean)      => registry.register(bean)
      case PlanStep.Create(definition) => registry.register(instantiator.instantiate(definition, registry))
    }
}
