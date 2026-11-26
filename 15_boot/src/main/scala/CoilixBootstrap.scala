package dev.coilix.boot

import cats.effect.IO
import cats.effect.Resource
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
   * The context built from the components in `basePackages`, the enabled modules, the `extraDefinitions` and the ready
   * `singletons`, as a `Resource` released by [[dev.coilix.context.ConfigurableApplicationContext.shutdown]]. The
   * [[dev.coilix.context.ContextLifecycleListener]] beans are not notified yet: the caller runs
   * [[dev.coilix.context.ConfigurableApplicationContext.start]] in `IO`, or starts the context with
   * [[dev.coilix.context.SynchronousLifecycle.started]].
   *
   * @return
   *   a resource that fails with a [[dev.coilix.context.NoSuchBeanException]] when a required dependency has no matching bean and
   *   with a [[dev.coilix.context.CircularDependencyException]] when beans depend on each other in a cycle
   */
  final def start(
    basePackages:     List[String],
    extraDefinitions: List[BeanDefinition],
    singletons:       List[RegisteredBean]
  ): Resource[IO, ConfigurableApplicationContext] =
    start(basePackages, extraDefinitions, singletons, BeanReplacements.none)

  /**
   * The context like the other `start`, but without the discovered beans that `replacements` replace, so the ready
   * `singletons` take their place. The `extraDefinitions` are never left out.
   *
   * @return
   *   a resource that fails like the other `start`, also with an [[dev.coilix.boot.UnmatchedBeanReplacementException]] when a
   *   replaced type matches no bean discovered by scanning or in modules and with a
   *   [[dev.coilix.boot.CollectedComponentConflictException]] when a class has a component annotation and an annotation
   *   collected by a module
   */
  def start(
    basePackages:     List[String],
    extraDefinitions: List[BeanDefinition],
    singletons:       List[RegisteredBean],
    replacements:     BeanReplacements
  ): Resource[IO, ConfigurableApplicationContext]
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
