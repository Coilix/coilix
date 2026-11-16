package dev.coilix.context

import dev.coilix.config.Environment

/**
 * A condition that a bean definition must meet to be added to the context, such as an active profile.
 *
 * Conditions are read from annotations by a [[dev.coilix.context.ConditionScanner]] and checked by a
 * [[dev.coilix.context.DefinitionFilter]] before the beans are created.
 */
trait BeanCondition {

  /** Whether the condition holds in `environment`. */
  def matches(environment: Environment): Boolean

  /** What the condition requires, for example `profile dev is active`, used to explain why a bean was left out. */
  def description: String
}
