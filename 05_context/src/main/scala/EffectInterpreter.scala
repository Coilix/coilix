package dev.coilix.context

import cats.effect.IO
import cats.effect.Resource

/**
 * Creates the beans of a [[dev.coilix.context.ContextPlan]] from the first level that has a bean made by an effect, for an effect
 * module such as `coilix-concurrency`.
 *
 * The [[dev.coilix.context.ApplicationContextBuilder]] creates the levels before that one itself, so beans the interpreter needs
 * already exist in `registry`. A module gives its interpreter through [[dev.coilix.context.CoilixModule.effectInterpreter]].
 */
trait EffectInterpreter {

  /**
   * The beans of `levels`, each level after the previous one, added to `registry`, as a `Resource`.
   *
   * Releasing the resource closes what the interpreter opened, including the `@PreDestroy` methods of the beans it created, so
   * these beans are registered without destroy methods. The context releases it once when it closes, before it destroys the beans
   * created without the interpreter. When creation fails, the resource releases what it opened before it fails.
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
  ): Resource[IO, BeanRegistry]
}
