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
import dev.coilix.context.ResourceT

/** Scans for components and builds the application context. */
trait CoilixBootstrap {

  /**
   * The context built from the components in `basePackages`, the enabled modules, the `extraDefinitions` and the ready
   * `singletons`, as a `Resource` released by [[dev.coilix.context.ConfigurableApplicationContext.shutdown]]. The
   * [[dev.coilix.context.ContextLifecycleListener]] beans are not notified yet: the caller runs
   * [[dev.coilix.context.ConfigurableApplicationContext.start]] in `IO`, or starts the context with
   * [[dev.coilix.context.SynchronousLifecycle.started]].
   *
   * @return
   *   the context, or a [[dev.coilix.boot.StartupError]] when it cannot be built: a
   *   [[dev.coilix.boot.StartupError.ConflictingEffectInterpreters]] when several modules give an effect interpreter, and a
   *   [[dev.coilix.boot.StartupError.Context]] with the [[dev.coilix.context.ContextError]] of the container, for example when a
   *   required dependency has no matching bean
   */
  final def start(
    basePackages:     List[String],
    extraDefinitions: List[BeanDefinition],
    singletons:       List[RegisteredBean]
  ): ResourceT[StartupError, ConfigurableApplicationContext] =
    start(basePackages, extraDefinitions, singletons, BeanReplacements.none)

  /**
   * The context like the other `start`, but without the discovered beans that `replacements` replace, so the ready
   * `singletons` take their place. The `extraDefinitions` are never left out.
   *
   * @return
   *   the context or an error like the other `start`, also a [[dev.coilix.boot.StartupError.UnmatchedBeanReplacement]] when a
   *   replaced type matches no bean discovered by scanning or in modules, a
   *   [[dev.coilix.boot.StartupError.CollectedComponentConflict]] when a class has a component annotation and an annotation
   *   collected by a module, and a [[dev.coilix.boot.StartupError.CollectedComponentRequired]] when a bean requires such a class
   */
  def start(
    basePackages:     List[String],
    extraDefinitions: List[BeanDefinition],
    singletons:       List[RegisteredBean],
    replacements:     BeanReplacements
  ): ResourceT[StartupError, ConfigurableApplicationContext]
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
