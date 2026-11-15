package dev.coilix.config
package enums

/**
 * The cases of an [[dev.coilix.annotation.Enumerated]] enum and their names.
 *
 * Instances come from an [[dev.coilix.config.enums.EnumResolver]].
 *
 * @param enumType
 *   the annotated enum
 * @param cases
 *   the cases in the order of declaration
 * @param ignoreCase
 *   whether a name matches regardless of letter case
 */
final case class EnumDefinition(enumType: Class[?], cases: List[EnumCase], ignoreCase: Boolean = false) {

  /** The names of the cases, without aliases. */
  def names: List[String] =
    cases.map(_.name)

  /** The case with the name or alias `name`. */
  def find(name: String): Option[AnyRef] =
    cases.find(_.allNames.exists(matches(name))).map(_.value)

  /**
   * The case with the name or alias `name`.
   *
   * @throws dev.coilix.config.enums.UnknownEnumValueException
   *   with the allowed names, if no case has this name
   */
  def valueOf(name: String): AnyRef =
    find(name).getOrElse(throw unknown(name))

  /**
   * The name of `value`, which must be a case of this enum.
   *
   * @throws java.lang.IllegalArgumentException
   *   if `value` is not a case of this enum
   */
  def nameOf(value: AnyRef): String =
    cases
      .find(_.value == value)
      .map(_.name)
      .getOrElse(throw IllegalArgumentException(s"$value is not a case of ${enumType.getName}"))

  private def matches(name: String)(candidate: String): Boolean =
    if (ignoreCase) candidate.equalsIgnoreCase(name) else candidate == name

  private def unknown(name: String): UnknownEnumValueException =
    UnknownEnumValueException(s"Unknown ${enumType.getName} '$name', expected one of: ${names.mkString(", ")}")
}
