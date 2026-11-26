package dev.coilix.boot

import dev.coilix.config.ComponentProvider
import dev.coilix.config.ConfigPath
import dev.coilix.context.ApplicationContextBuilder
import dev.coilix.context.BeanDefinition
import dev.coilix.context.BeanDefinitionFactory
import dev.coilix.context.ClassPathScanner
import dev.coilix.context.ComponentCollector
import dev.coilix.context.ConfigurableApplicationContext
import dev.coilix.context.ModuleLoader
import dev.coilix.context.RegisteredBean

/** Scans for components and builds the application context. */
trait CoilixBootstrap {

  /**
   * Builds the context from the components in `basePackages`, the enabled modules, the `extraDefinitions` and the ready
   * `singletons`. The [[dev.coilix.context.ContextLifecycleListener]] beans are not notified yet: the caller runs
   * [[dev.coilix.context.ConfigurableApplicationContext.start]] in `IO`, or starts the context with
   * [[dev.coilix.context.SynchronousLifecycle.started]].
   *
   * @throws dev.coilix.context.NoSuchBeanException
   *   if a required dependency has no matching bean
   * @throws dev.coilix.context.CircularDependencyException
   *   if beans depend on each other in a cycle
   */
  final def start(
    basePackages:     List[String],
    extraDefinitions: List[BeanDefinition],
    singletons:       List[RegisteredBean]
  ): ConfigurableApplicationContext =
    start(basePackages, extraDefinitions, singletons, BeanReplacements.none)

  /**
   * Builds the context like the other `start`, but leaves out the discovered beans that `replacements` replace, so the ready
   * `singletons` take their place. The `extraDefinitions` are never left out.
   *
   * @throws dev.coilix.boot.UnmatchedBeanReplacementException
   *   if a replaced type matches no bean discovered by scanning or in modules
   * @throws dev.coilix.boot.CollectedComponentConflictException
   *   if a class has a component annotation and an annotation collected by a module
   * @throws dev.coilix.context.NoSuchBeanException
   *   if a required dependency has no matching bean
   * @throws dev.coilix.context.CircularDependencyException
   *   if beans depend on each other in a cycle
   */
  def start(
    basePackages:     List[String],
    extraDefinitions: List[BeanDefinition],
    singletons:       List[RegisteredBean],
    replacements:     BeanReplacements
  ): ConfigurableApplicationContext
}

/** Configuration path and default implementation of [[dev.coilix.boot.CoilixBootstrap]]. */
object CoilixBootstrap {

  /** Replace the component in the configuration under `coilix.boot.bootstrap`, with a `class` key naming the implementation. */
  given ConfigPath[CoilixBootstrap] = ConfigPath[CoilixBootstrap]("coilix.boot.bootstrap")

  /** The configured implementation, or [[dev.coilix.boot.DefaultCoilixBootstrap]] when `coilix.boot.bootstrap` is not set. */
  def default(using components: ComponentProvider): CoilixBootstrap =
    components.get[CoilixBootstrap](
      DefaultCoilixBootstrap(
        ClassPathScanner.default,
        ApplicationContextBuilder.default,
        ModuleLoader.default,
        BeanDefinitionFactory.default,
        ComponentCollector.default
      )
    )
}
