package dev.coilix.context

import dev.coilix.config.ComponentProvider
import dev.coilix.config.ConfigPath

/** Turns the classes collected for modules into the definition of the `collectedComponents` bean. */
trait ComponentCollector {

  /**
   * The definition of a [[dev.coilix.context.CollectedComponents]] bean holding `classes`.
   *
   * The definition depends on every bean the collected classes need, so the context creates those beans first and reports a
   * missing one when it starts.
   */
  def collect(classes: List[Class[?]]): BeanDefinition
}

/** Configuration path and default implementation of [[dev.coilix.context.ComponentCollector]]. */
object ComponentCollector {

  /**
   * Replace the component in the configuration under `coilix.context.component-collector`, with a `class` key naming the
   * implementation.
   */
  given ConfigPath[ComponentCollector] = ConfigPath[ComponentCollector]("coilix.context.component-collector")

  /**
   * The configured implementation, or a [[dev.coilix.context.DefaultComponentCollector]] built from the default definition
   * factory, definition filter, bean post-processor and bean destroyer when `coilix.context.component-collector` is not set.
   */
  def default(using components: ComponentProvider): ComponentCollector =
    components.get[ComponentCollector](
      DefaultComponentCollector(
        BeanDefinitionFactory.default,
        DefinitionFilter.default,
        BeanPostProcessor.default,
        BeanDestroyer.default
      )
    )
}
