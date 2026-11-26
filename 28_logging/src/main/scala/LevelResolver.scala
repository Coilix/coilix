package dev.coilix.logging

/**
 * Chooses the level of a logger: the level set for the longest prefix of its name that ends at a dot, or the root level.
 *
 * A level set for `org.apache` applies to `org.apache.kafka.Consumer`, and a level set for `org.apache.kafka` wins over it.
 *
 * @param root
 *   the level of loggers that no entry matches
 * @param levels
 *   levels by package or logger name
 */
case class LevelResolver(root: LogLevel, levels: Map[String, LogLevel]) {

  /** The level of the logger named `name`. */
  def levelOf(name: String): LogLevel =
    levels
      .filter((prefix, _) => matches(prefix, name))
      .maxByOption((prefix, _) => prefix.length)
      .fold(root)(_._2)

  private def matches(prefix: String, name: String): Boolean =
    name == prefix || name.startsWith(s"$prefix.") || name.startsWith(s"$prefix$$")
}
