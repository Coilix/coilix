package dev.coilix.context

import dev.coilix.config.ComponentProvider
import dev.coilix.config.ConfigPath

/**
 * Creates the beans from their definitions and returns the context that holds them.
 *
 * [[dev.coilix.context.ApplicationContextBuilder.create]] only creates the beans, so the owner runs
 * [[dev.coilix.context.ConfigurableApplicationContext.start]] in `IO`. `build` also starts the context, blocking until the
 * [[dev.coilix.context.ContextLifecycleListener]] beans are notified, for code outside `IO`.
 */
trait ApplicationContextBuilder {

  /**
   * Creates every bean in `definitions` in dependency order, without notifying the
   * [[dev.coilix.context.ContextLifecycleListener]] beans.
   *
   * @param singletons
   *   ready instances added to the context before any bean is created, such as the environment
   * @param interpreter
   *   creates the beans from the first level with a bean made by an effect; without one such beans stop the build with an
   *   [[dev.coilix.context.EffectfulBeanException]]
   * @throws dev.coilix.context.NoSuchBeanException
   *   if a required dependency has no matching bean
   * @throws dev.coilix.context.CircularDependencyException
   *   if beans depend on each other in a cycle
   */
  def create(
    definitions: List[BeanDefinition],
    singletons:  List[RegisteredBean],
    interpreter: Option[EffectInterpreter]
  ): ConfigurableApplicationContext

  /**
   * Creates every bean like [[dev.coilix.context.ApplicationContextBuilder.create]] and starts the context with
   * [[dev.coilix.context.SynchronousLifecycle.started]]. A failing listener closes the context and its exception stops the
   * build.
   */
  final def build(
    definitions: List[BeanDefinition],
    singletons:  List[RegisteredBean],
    interpreter: Option[EffectInterpreter]
  ): ConfigurableApplicationContext =
    SynchronousLifecycle.started(create(definitions, singletons, interpreter))

  /** Creates and starts every bean in `definitions` after the `singletons`, without an effect interpreter. */
  final def build(definitions: List[BeanDefinition], singletons: List[RegisteredBean]): ConfigurableApplicationContext =
    build(definitions, singletons, None)

  /** Creates and starts every bean in `definitions`, without ready instances. */
  final def build(definitions: List[BeanDefinition]): ConfigurableApplicationContext = build(definitions, Nil)
}

/** Configuration path and default implementation of [[dev.coilix.context.ApplicationContextBuilder]]. */
object ApplicationContextBuilder {

  /** Replace the component in the configuration under `coilix.context.application-context-builder`. */
  given ConfigPath[ApplicationContextBuilder] = ConfigPath[ApplicationContextBuilder]("coilix.context.application-context-builder")

  /**
   * The configured implementation, or a [[dev.coilix.context.DefaultApplicationContextBuilder]] built from the default context
   * planner, bean instantiator, candidate selector and bean destroyer.
   */
  def default(using components: ComponentProvider): ApplicationContextBuilder =
    components.get[ApplicationContextBuilder](
      DefaultApplicationContextBuilder(
        ContextPlanner.default,
        BeanInstantiator.default,
        CandidateSelector.default,
        BeanDestroyer.default
      )
    )
}
