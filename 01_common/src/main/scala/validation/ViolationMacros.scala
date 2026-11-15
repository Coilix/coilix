package dev.coilix.common
package validation

import scala.quoted.Expr
import scala.quoted.Quotes
import scala.quoted.Type
import scala.quoted.Varargs

/**
 * Compile-time checks behind `Violation(message)` and `ViolationTemplate(template)(arguments)`.
 *
 * The code of the violation comes from the name of the `val` that holds it: `blankName` becomes `blank.name`.
 */
private[validation] object ViolationMacros {

  private val wordBoundary = "(?<=[a-z0-9])(?=[A-Z])|(?<=[A-Z])(?=[A-Z][a-z])"

  def violation(message: Expr[String])(using quotes: Quotes): Expr[Violation] = {
    val text = literal(message, "Violation message")
    if (text.isEmpty) quotes.reflect.report.errorAndAbort("Violation message must not be empty")
    if (text.contains("%")) quotes.reflect.report.errorAndAbort(
      "Violation message must not contain %, use ViolationTemplate for messages with values"
    )
    '{ new Violation(${ Expr(code("Violation")) }, ${ Expr(text) }) }
  }

  def template[T: Type](template: Expr[String], arguments: Expr[Seq[T => String]])(using
    quotes: Quotes
  ): Expr[ViolationTemplate[T]] = {
    val text      = literal(template, "Violation template")
    val functions = argumentList(arguments)
    checkTemplate(text, functions.size)
    '{ new ViolationTemplate[T](${ Expr(code("ViolationTemplate")) }, ${ Expr(text) }, ${ Expr.ofList(functions) }) }
  }

  private def checkTemplate(text: String, arguments: Int)(using quotes: Quotes): Unit = {
    val placeholders = "%s".r.findAllMatchIn(text).size
    if (text.matches("(?s).*%(?!s).*")) quotes.reflect.report.errorAndAbort(
      s"Violation template '$text' may contain only %s placeholders"
    )
    if (placeholders != arguments) quotes.reflect.report.errorAndAbort(
      s"Violation template '$text' has $placeholders placeholders, but $arguments arguments"
    )
  }

  private def argumentList[T](arguments: Expr[Seq[T => String]])(using quotes: Quotes): List[Expr[T => String]] =
    arguments match {
      case Varargs(functions) => functions.toList
      case _                  => quotes.reflect.report.errorAndAbort("Pass the template arguments directly, not as a sequence")
    }

  private def literal(text: Expr[String], what: String)(using quotes: Quotes): String =
    text.value.getOrElse(quotes.reflect.report.errorAndAbort(s"$what must be a string literal"))

  private def code(what: String)(using quotes: Quotes): String = {
    import quotes.reflect.*
    List(Symbol.spliceOwner, Symbol.spliceOwner.owner)
      .find(owner => owner.isValDef && !owner.flags.is(Flags.Synthetic))
      .map(owner => words(owner.name))
      .getOrElse(report.errorAndAbort(s"$what must be assigned directly to a val or lazy val, whose name becomes its code"))
  }

  private def words(name: String): String =
    name.stripPrefix("_").stripPrefix("$").split(wordBoundary).map(_.toLowerCase(java.util.Locale.ROOT)).mkString(".")
}
