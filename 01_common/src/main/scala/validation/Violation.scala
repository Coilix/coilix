package dev.coilix.common
package validation

/**
 * One broken rule found by a [[dev.coilix.common.validation.Validator]].
 *
 * ```scala
 * import dev.coilix.common.validation.Violation
 *
 * val blankName = Violation("Name must not be blank")
 * ```
 *
 * `Violation(message)` takes the code from the name of the `val`, here `blank.name`, and checks the message when the code
 * compiles. `Violation(code, message)` sets the code explicitly.
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

/** Creates violations whose code comes from the name of the `val` that holds them. */
object Violation {

  /**
   * A violation with `message`, whose code is the name of the enclosing `val` split into lower-case words joined by dots,
   * so that `val listTooLong = Violation(...)` has the code `list.too.long`.
   *
   * Compilation fails when `message` is not a string literal, is empty or contains `%`, or when the result is not assigned
   * directly to a `val` or `lazy val`.
   */
  inline def apply(inline message: String): Violation =
    ${ ViolationMacros.violation('message) }
}
