package dev.coilix.context

import dev.coilix.annotation.Slf4s
import scala.util.control.NonFatal

/**
 * Builds a [[dev.coilix.context.DefaultApplicationContext]] by running a [[dev.coilix.context.ContextPlan]] synchronously.
 *
 * The context itself is provided as `applicationContext`, before the given singletons. The plan from the
 * [[dev.coilix.context.ContextPlanner]] is checked before any bean is created: a bean whose factory returns an effect stops the
 * build with an [[dev.coilix.context.EffectfulBeanException]], because this builder does not run effects. Each other bean is
 * created with the [[dev.coilix.context.BeanInstantiator]], at once for an eager bean, and as a
 * [[dev.coilix.context.DeferredBean]] created on lookup for a lazy bean or a prototype. A singleton that requires a prototype
 * directly gets one instance of it, which is logged as a warning, because it usually means a missing `Provider`. When every
 * eager bean exists, the
 * [[dev.coilix.context.ContextLifecycleListener]] beans are notified in creation order. A failing listener closes the context
 * and its exception stops the build.
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
@Slf4s
final class DefaultApplicationContextBuilder(
  planner:      ContextPlanner,
  instantiator: BeanInstantiator,
  selector:     CandidateSelector,
  destroyer:    BeanDestroyer
) extends ApplicationContextBuilder {

  private val contextBeanName = "applicationContext"

  override def build(definitions: List[BeanDefinition], singletons: List[RegisteredBean]): ConfigurableApplicationContext = {
    lazy val context:  ConfigurableApplicationContext = DefaultApplicationContext(registry, destroyer)
    lazy val registry: BeanRegistry                   = executed(planner.plan(definitions, contextBean(context) :: singletons))
    val _ = registry
    started(context)
  }

  private def executed(plan: ContextPlan): BeanRegistry = {
    val completion = RegistryCompletion()
    val registry   = execute(plan, completion)
    completion.complete(registry)
    registry
  }

  private def started(context: ConfigurableApplicationContext): ConfigurableApplicationContext =
    try {
      context.createdBeans.map(
        _.instance
      ).collect { case listener: ContextLifecycleListener => listener }.foreach(_.onContextStarted(context))
      context
    } catch {
      case NonFatal(failure) =>
        context.close()
        throw failure
    }

  private def contextBean(context: ApplicationContext): RegisteredBean =
    RegisteredBean.ofClass(contextBeanName, context.getClass, context)

  private def execute(plan: ContextPlan, completion: RegistryCompletion): BeanRegistry =
    if (plan.effectful.nonEmpty) throw EffectfulBeanException(plan.effectful)
    else {
      warnAboutPrototypes(plan.creationOrder)
      plan.steps.foldLeft(ImmutableBeanRegistry.empty(selector, completion))(execute)
    }

  private def execute(registry: BeanRegistry, step: PlanStep): BeanRegistry =
    step match {
      case PlanStep.Provide(bean)      => registry.register(bean)
      case PlanStep.Create(definition) => created(registry, definition)
    }

  private def created(registry: BeanRegistry, definition: BeanDefinition): BeanRegistry =
    if (definition.creation.atStartup) registry.register(instantiator.instantiate(definition, registry))
    else registry.defer(DeferredBean.of(definition)(lookup => instantiator.instantiate(definition, lookup)))

  private def warnAboutPrototypes(definitions: List[BeanDefinition]): Unit =
    definitions.filter(_.creation != BeanCreation.Prototype).foreach { definition =>
      definition.dependencies
        .collect { case Dependency.Required(beanType) => beanType }
        .flatMap(beanType => selector.select(beanType, definitions).filter(_.creation == BeanCreation.Prototype))
        .foreach(prototype => warnAboutPrototype(definition, prototype))
    }

  private def warnAboutPrototype(definition: BeanDefinition, prototype: BeanDefinition): Unit =
    log.warn(
      s"Bean '${definition.beanName}' requires the prototype '${prototype.beanName}' directly, so it keeps one instance of it. " +
        s"Inject Provider[${prototype.beanType}] to get a new instance each time"
    )
}
