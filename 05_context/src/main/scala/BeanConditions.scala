package dev.coilix.context

/**
 * The conditions of a bean definition.
 *
 * @param conditions
 *   conditions that must all hold
 * @param onlyIfMissing
 *   whether the bean is left out when another bean provides its type, from
 *   [[dev.coilix.annotation.ConditionalOnMissingBean]]
 * @param missingTypes
 *   the types another bean must provide to leave this bean out, or empty for the type of the bean itself
 */
final case class BeanConditions(
  conditions:    List[BeanCondition] = Nil,
  onlyIfMissing: Boolean             = false,
  missingTypes:  List[Class[?]]      = Nil
)

/** Constants for [[dev.coilix.context.BeanConditions]]. */
object BeanConditions {

  /** No conditions: the bean is always added. */
  val none: BeanConditions = BeanConditions()
}
