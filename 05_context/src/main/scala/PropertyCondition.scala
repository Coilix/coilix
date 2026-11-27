package dev.coilix.context

import dev.coilix.config.Environment

/**
 * Holds when a configuration key has the expected value. Read from [[dev.coilix.annotation.ConditionalOnProperty]].
 *
 * @param name
 *   the configuration key
 * @param havingValue
 *   the expected value, compared without letter case, or an empty text to accept any value except `false`
 * @param matchIfMissing
 *   whether the condition holds when the key is not set
 */
case class PropertyCondition(name: String, havingValue: String = "", matchIfMissing: Boolean = false)
    extends BeanCondition {

  override def matches(environment: Environment): Boolean =
    environment.get[String](name).fold(matchIfMissing)(accepts)

  override def description: String =
    if (havingValue.isEmpty) s"configuration '$name' is set and not false"
    else s"configuration '$name' is '$havingValue'"

  private def accepts(value: String): Boolean =
    if (havingValue.isEmpty) !value.equalsIgnoreCase("false") else value.equalsIgnoreCase(havingValue)
}
