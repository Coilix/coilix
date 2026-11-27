package dev.coilix.common
package validation

/**
 * One rule of a [[dev.coilix.common.validation.Validator]]: a condition and the violation reported when it fails.
 *
 * @param condition
 *   returns `true` when the value follows the rule
 * @param violation
 *   reported when `condition` returns `false`
 */
case class Statement[T](condition: T => Boolean, violation: Violation) {

  /** Whether `value` follows the rule. */
  def holds(value: T): Boolean =
    condition(value)

  /** The same rule, checked on the part of a larger value that `part` selects. */
  def contramap[S](part: S => T): Statement[S] =
    Statement(value => condition(part(value)), violation)
}
