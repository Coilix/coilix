package dev.coilix.context

import cats.effect.IO
import cats.effect.Resource
import dev.coilix.annotation.Slf4s
import java.util.concurrent.atomic.AtomicReference

/**
 * Builds a [[dev.coilix.context.DefaultApplicationContext]] by running a [[dev.coilix.context.ContextPlan]] in `IO`.
 *
 * The context itself is provided as `applicationContext`, before the given singletons. The plan from the
 * [[dev.coilix.context.ContextPlanner]] is checked before any bean is created: an effectful bean, whose factory or lifecycle
 * method returns an effect, stops the build with an [[dev.coilix.context.EffectfulBeanException]] unless an
 * [[dev.coilix.context.EffectInterpreter]] is given. A bean created by the builder itself whose instance turns out to have a
 * lifecycle method returning an effect, declared only on its implementation, stops the build with a
 * [[dev.coilix.context.BeanCreationException]]. The
 * interpreter opens the levels of the plan from the first one with such a bean as a `Resource`, and the context releases it when
 * it closes, before it destroys the other beans; when it fails, the beans created before are destroyed. Planning and the
 * factories run in `IO.blocking`, because they use reflection and call constructors that may block. Each other bean is
 * created with the [[dev.coilix.context.BeanInstantiator]], at once for an eager bean, and as a
 * [[dev.coilix.context.DeferredBean]] created on lookup for a lazy bean or a prototype. A singleton that requires a prototype
 * directly gets one instance of it, which is logged as a warning, because it usually means a missing `Provider`. When every
 * eager bean exists, the context is given without notifying the [[dev.coilix.context.ContextLifecycleListener]] beans; its
 * owner runs [[dev.coilix.context.ConfigurableApplicationContext.start]].
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

  override def create(
    definitions: List[BeanDefinition],
    singletons:  List[RegisteredBean],
    interpreter: Option[EffectInterpreter]
  ): Resource[IO, ConfigurableApplicationContext] =
    Resource.make(created(definitions, singletons, interpreter))(_.shutdown)

  private def created(
    definitions: List[BeanDefinition],
    singletons:  List[RegisteredBean],
    interpreter: Option[EffectInterpreter]
  ): IO[ConfigurableApplicationContext] =
    IO.defer {
      val beans   = AtomicReference(Option.empty[(BeanRegistry, IO[Unit])])
      val context = DefaultApplicationContext(registryOf(beans), destroyer, releaseOf(beans))
      IO.blocking(planner.plan(definitions, contextBean(context) :: singletons))
        .flatMap(plan => executed(plan, interpreter))
        .flatMap(opened => IO(beans.set(Some(opened))))
        .as(context)
    }

  private def registryOf(beans: AtomicReference[Option[(BeanRegistry, IO[Unit])]]): BeanRegistry =
    beans.get.fold(throw IllegalStateException("The beans of the context are still being created"))(_._1)

  private def releaseOf(beans: AtomicReference[Option[(BeanRegistry, IO[Unit])]]): IO[Unit] =
    IO(beans.get).flatMap(_.fold(IO.unit)(_._2))

  private def executed(plan: ContextPlan, interpreter: Option[EffectInterpreter]): IO[(BeanRegistry, IO[Unit])] = {
    val completion = RegistryCompletion()
    execute(plan, interpreter, completion).flatTap((registry, _) => IO(completion.complete(registry)))
  }

  private def contextBean(context: ApplicationContext): RegisteredBean =
    RegisteredBean.ofClass(contextBeanName, context.getClass, context)

  private def execute(
    plan:        ContextPlan,
    interpreter: Option[EffectInterpreter],
    completion:  RegistryCompletion
  ): IO[(BeanRegistry, IO[Unit])] = {
    val effects             = interpreter.filter(_ => plan.effectful.nonEmpty)
    val (direct, effectful) = plan.levels.span(_.forall(!_.isEffectful))
    IO.raiseWhen(plan.effectful.nonEmpty && effects.isEmpty)(EffectfulBeanException(plan.effectful)) *>
      IO.blocking {
        warnAboutPrototypes(plan.creationOrder)
        direct.flatten.foldLeft(provided(plan, completion))(created)
      }.flatMap(registry => effects.fold(IO.pure((registry, IO.unit)))(interpreted(effectful, registry)))
  }

  private def provided(plan: ContextPlan, completion: RegistryCompletion): BeanRegistry =
    plan.provided.foldLeft(ImmutableBeanRegistry.empty(selector, completion))(_.register(_))

  private def interpreted(levels: List[List[BeanDefinition]], registry: BeanRegistry)(
    interpreter: EffectInterpreter
  ): IO[(BeanRegistry, IO[Unit])] =
    IO.defer(interpreter.interpret(levels, registry, instantiator, destroyer).allocated)
      .onError(_ => IO.blocking(destroyer.destroy(registry.beans)))

  private def created(registry: BeanRegistry, definition: BeanDefinition): BeanRegistry =
    if (definition.creation.atStartup) registry.register(instantiated(definition, registry))
    else registry.defer(DeferredBean.of(definition)(lookup => instantiated(definition, lookup)))

  private def instantiated(definition: BeanDefinition, registry: BeanRegistry): RegisteredBean = {
    val bean = instantiator.instantiate(definition, registry)
    bean.copy(lifecycle = bean.lifecycle.withoutEffects(bean.beanName))
  }

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
