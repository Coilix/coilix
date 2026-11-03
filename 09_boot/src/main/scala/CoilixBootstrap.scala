package dev.coilix.boot

import dev.coilix.config.ComponentProvider
import dev.coilix.config.ConfigPath
import dev.coilix.context.ApplicationContextBuilder
import dev.coilix.context.BeanDefinition
import dev.coilix.context.ClassPathScanner
import dev.coilix.context.ConfigurableApplicationContext
import dev.coilix.context.RegisteredBean

/** Scans for components and builds the application context. */
trait CoilixBootstrap {

  /**
   * Builds the context from the components in `basePackages`, the `extraDefinitions` and the ready `singletons`.
   *
   * @throws dev.coilix.context.NoSuchBeanException
   *   if a required dependency has no matching bean
   * @throws dev.coilix.context.CircularDependencyException
   *   if beans depend on each other in a cycle
   */
  def start(
    basePackages:     List[String],
    extraDefinitions: List[BeanDefinition],
    singletons:       List[RegisteredBean]
  ): ConfigurableApplicationContext
}

/** Configuration path and default implementation of [[dev.coilix.boot.CoilixBootstrap]]. */
object CoilixBootstrap {

  /** Replace the component in the configuration under `coilix.boot.bootstrap`, with a `class` key naming the implementation. */
  given ConfigPath[CoilixBootstrap] = ConfigPath[CoilixBootstrap]("coilix.boot.bootstrap")

  /** The configured implementation, or [[dev.coilix.boot.DefaultCoilixBootstrap]] when `coilix.boot.bootstrap` is not set. */
  def default(using components: ComponentProvider): CoilixBootstrap =
    components.get[CoilixBootstrap](DefaultCoilixBootstrap(ClassPathScanner.default, ApplicationContextBuilder.default))
}
