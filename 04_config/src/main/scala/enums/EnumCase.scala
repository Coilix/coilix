package dev.coilix.config
package enums

/**
 * One case of an [[dev.coilix.annotation.Enumerated]] enum.
 *
 * @param value
 *   the case itself
 * @param name
 *   the name used to read and write the case, from [[dev.coilix.annotation.EnumName]] or the name in the code
 * @param aliases
 *   other names that also find the case when reading
 */
case class EnumCase(value: AnyRef, name: String, aliases: List[String] = Nil) {

  /** The name followed by the aliases. */
  def allNames: List[String] =
    name :: aliases
}
