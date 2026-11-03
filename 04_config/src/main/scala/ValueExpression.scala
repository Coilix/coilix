package dev.coilix.config

import scala.util.matching.Regex

enum ValueExpression {
  case Literal(value: String)
  case Placeholder(key: String, default: Option[String])
}

object ValueExpression {
  private val placeholder: Regex = """\$\{([^:}]+)(?::([^}]*))?}""".r

  def parse(raw: String): ValueExpression =
    raw.trim match {
      case placeholder(key, default) => Placeholder(key.trim, Option(default))
      case _                         => Literal(raw)
    }
}
