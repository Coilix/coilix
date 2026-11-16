package dev.coilix.context

import dev.coilix.config.ComponentProvider
import dev.coilix.config.ConfigPath

/** Reads the conditions of a bean definition while the definition is created. */
trait ConditionScanner {

  /** The conditions of `definition`, from its class or, for a `@Bean` method, from the method and its configuration class. */
  def scan(definition: BeanDefinition): BeanConditions
}

/** Configuration path and default implementation of [[dev.coilix.context.ConditionScanner]]. */
object ConditionScanner {

  /** Replace the component in the configuration under `coilix.context.condition-scanner`, with a `class` key naming the implementation. */
  given ConfigPath[ConditionScanner] = ConfigPath[ConditionScanner]("coilix.context.condition-scanner")

  /**
   * The configured implementation, or an [[dev.coilix.context.AnnotatedConditionScanner]] that uses the context class loader of
   * the current thread when `coilix.context.condition-scanner` is not set.
   */
  def default(using components: ComponentProvider): ConditionScanner =
    components.get[ConditionScanner](AnnotatedConditionScanner(Thread.currentThread().getContextClassLoader))
}
