package dev.coilix.config

import scala.util.matching.Regex

/** A parsed `@Value` expression. */
enum ValueExpression {

  /** A fixed value used as it is, for example `@Value("8080")`. */
  case Literal(value: String)

  /**
   * A reference to a configuration key, written as `${key}` or `${key:default}`.
   *
   * @param key
   *   the configuration path to look up
   * @param default
   *   the value used when `key` is not set; `Some("")` for `${key:}`
   */
  case Placeholder(key: String, default: Option[String])
}

/** Parsing of `@Value` expressions. */
object ValueExpression {
  private val placeholder: Regex = """\$\{([^:}]+)(?::([^}]*))?}""".r

  /**
   * Parses `raw` as a placeholder when the whole text, ignoring surrounding spaces, has the form `${key}` or `${key:default}`.
   * Any other text is a [[dev.coilix.config.ValueExpression.Literal]].
   */
  def parse(raw: String): ValueExpression =
    raw.trim match {
      case placeholder(key, default) => Placeholder(key.trim, Option(default))
      case _                         => Literal(raw)
    }
}
