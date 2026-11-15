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
 * val tooLong = ViolationTemplate[Int]("user.name.too-long", "Name must have at most %s characters")(_.toString)
 *
 * tooLong(40).message
 * ```
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

/** Factory for [[dev.coilix.common.validation.ViolationTemplate]]. */
object ViolationTemplate {

  /** A template whose `%s` placeholders are filled by `arguments`, in order. */
  def apply[T](code: String, template: String)(arguments: (T => String)*): ViolationTemplate[T] =
    new ViolationTemplate(code, template, arguments.toList)

  private def placeholders(template: String): Int =
    "%s".r.findAllMatchIn(template).size
}
