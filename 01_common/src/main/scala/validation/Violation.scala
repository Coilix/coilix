package dev.coilix.common
package validation

/**
 * One broken rule found by a [[dev.coilix.common.validation.Validator]].
 *
 * ```scala
 * import dev.coilix.common.validation.Violation
 *
 * val blankName = Violation("user.name.blank", "Name must not be blank")
 * ```
 *
 * @param code
 *   a stable identifier of the rule, for example for translations or API error codes
 * @param message
 *   a readable description of the problem
 * @param arguments
 *   the values inserted into the message, when it comes from a [[dev.coilix.common.validation.ViolationTemplate]]
 * @param path
 *   the field the violation refers to, such as `address.city`, or `None` for the whole value
 */
final case class Violation(code: String, message: String, arguments: List[String] = Nil, path: Option[String] = None) {

  /** The same violation, referring to the field `path`. */
  def withPath(path: String): Violation =
    copy(path = Some(path))

  /** The same violation, with `prefix` added in front of its path, so that `city` under `address` becomes `address.city`. */
  def under(prefix: String): Violation =
    withPath(path.fold(prefix)(field => s"$prefix.$field"))

  /** The message, preceded by the path when there is one. */
  override def toString: String =
    path.fold(message)(field => s"$field: $message")
}
