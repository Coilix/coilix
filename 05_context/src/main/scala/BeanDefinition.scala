package dev.coilix.context

import dev.coilix.common.GenericType

/**
 * Describes how to create one bean: its name, type, factory and dependencies.
 *
 * Definitions are created by a [[dev.coilix.context.BeanDefinitionFactory]] during component scanning and turned into beans by an
 * [[dev.coilix.context.ApplicationContextBuilder]].
 *
 * @param beanName
 *   unique name of the bean
 * @param beanType
 *   type under which the bean is registered and injected, with its type arguments
 * @param factory
 *   creates the instance from the resolved dependencies
 * @param dependencies
 *   arguments passed to `factory`, in order
 * @param isPrimary
 *   whether the bean wins when several beans match a requested type
 * @param lifecycle
 *   methods invoked after creation and when the context closes
 * @param conditions
 *   conditions that decide whether the bean is added to the context
 * @param result
 *   whether `factory` returns the instance or an effect that gives it, in which case `beanType` is the type of the value
 * @param creation
 *   when the instance is created and whether it is kept
 * @param dependsOn
 *   names of beans created before this one and destroyed after it, without being injected, from
 *   [[dev.coilix.annotation.DependsOn]]
 */
final case class BeanDefinition(
  beanName:     String,
  beanType:     GenericType,
  factory:      InstanceFactory,
  dependencies: List[Dependency],
  isPrimary:    Boolean,
  lifecycle:    LifecycleMethods = LifecycleMethods.none,
  conditions:   BeanConditions   = BeanConditions.none,
  result:       BeanResult       = BeanResult.Plain,
  creation:     BeanCreation     = BeanCreation.Eager,
  dependsOn:    List[String]     = Nil
) extends BeanDescriptor {

  /**
   * Whether only an [[dev.coilix.context.EffectInterpreter]] can create this bean: its factory returns an effect, or one of its
   * lifecycle methods returns an effect such as `IO[Unit]`.
   */
  def isEffectful: Boolean = result.isEffectful || lifecycle.hasEffects
}
