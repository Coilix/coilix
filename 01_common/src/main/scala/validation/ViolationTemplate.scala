package dev.coilix.common
package validation

/**
 * Creates violations whose message contains values taken from the validated data.
 *
 * Every `%s` in the template is replaced, in order, by the result of one argument function.
 *
 * ```scala
 * import dev.coilix.common.validation.ViolationTemplate
 *
 * val nameTooLong = ViolationTemplate[Int]("Name must have at most %s characters")(_.toString)
 *
 * nameTooLong(40).message
 * ```
 *
 * `ViolationTemplate(template)(arguments)` takes the code from the name of the `val`, here `name.too.long`, and checks the
 * template against the arguments when the code compiles. The constructor sets the code explicitly and checks the template
 * when it runs.
 *
 * @param code
 *   the code of every created violation
 * @param template
 *   the message with one `%s` for each argument function
 * @param arguments
 *   functions that turn the input into the inserted values
 * @throws java.lang.IllegalArgumentException
 *   if the number of `%s` placeholders differs from the number of argument functions, or the template contains another
 *   format specifier
 */
final class ViolationTemplate[T](val code: String, val template: String, arguments: List[T => String]) {

  require(!template.matches("(?s).*%(?!s).*"), s"Violation template '$template' may contain only %s placeholders")

  require(
    ViolationTemplate.placeholders(template) == arguments.size,
    s"Violation template '$template' has ${ViolationTemplate.placeholders(template)} placeholders, but ${arguments.size} arguments"
  )

  /** A violation with the values computed from `input`. */
  def apply(input: T): Violation = {
    val values = arguments.map(_(input))
    Violation(code, template.format(values*), values)
  }
}

/** Creates templates whose code comes from the name of the `val` that holds them. */
object ViolationTemplate {

  /**
   * A template whose `%s` placeholders are filled by `arguments`, in order, and whose code is the name of the enclosing
   * `val` split into lower-case words joined by dots.
   *
   * Compilation fails when `template` is not a string literal, contains a format specifier other than `%s`, has a different
   * number of placeholders than `arguments`, or when the result is not assigned directly to a `val` or `lazy val`.
   */
  inline def apply[T](inline template: String)(inline arguments: (T => String)*): ViolationTemplate[T] =
    ${ ViolationMacros.template[T]('template, 'arguments) }

  private def placeholders(template: String): Int =
    "%s".r.findAllMatchIn(template).size
}
