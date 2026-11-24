package dev.coilix.context

/**
 * Creates the beans of a [[dev.coilix.context.ContextPlan]] from the first level that has a bean made by an effect, for an effect
 * module such as `coilix-cats-effect`.
 *
 * The [[dev.coilix.context.ApplicationContextBuilder]] creates the levels before that one itself, so beans the interpreter needs,
 * such as the runtime of the effect, already exist in `registry`. A module gives its interpreter through
 * [[dev.coilix.context.CoilixModule.effectInterpreter]].
 */
trait EffectInterpreter {

  /**
   * Creates the beans of `levels`, each level after the previous one, and adds them to `registry`.
   *
   * The returned [[dev.coilix.context.InterpretedBeans]] releases what the interpreter opened, including the `@PreDestroy`
   * methods of the beans it created, so these beans are registered without destroy methods. When creation fails, the
   * interpreter releases what it opened before it throws.
   *
   * @param levels
   *   the remaining beans grouped by dependency level, as in [[dev.coilix.context.ContextPlan.levels]]
   * @param registry
   *   the ready instances and the beans of the lower levels
   * @param instantiator
   *   calls the factories and completes the beans
   * @param destroyer
   *   runs the `@PreDestroy` methods of a bean
   */
  def interpret(
    levels:       List[List[BeanDefinition]],
    registry:     BeanRegistry,
    instantiator: BeanInstantiator,
    destroyer:    BeanDestroyer
  ): InterpretedBeans
}
