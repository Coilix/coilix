package dev.coilix.context

/**
 * Changes bean definitions before the context is planned, for example to create a bean as a proxy.
 *
 * A module provides processors through [[dev.coilix.context.CoilixModule.definitionProcessors]], and the bootstrap applies
 * them to every discovered definition, in the order of the modules. A changed type or new dependencies are therefore seen by
 * conditions, bean replacements and dependency resolution.
 */
trait DefinitionProcessor {

  /**
   * The definition to use instead of `definition`, or `definition` itself when the processor does not apply to it.
   *
   * @throws dev.coilix.common.CoilixException
   *   if the definition cannot be processed, which stops the startup
   */
  def process(definition: BeanDefinition): BeanDefinition
}
