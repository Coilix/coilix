package dev.coilix.config
package enums

/** Recognizes [[dev.coilix.annotation.Enumerated]] enums and lists their cases with names. */
trait EnumResolver {

  /** Whether `candidate` is an enum annotated with [[dev.coilix.annotation.Enumerated]]. */
  def isEnum(candidate: Class[?]): Boolean

  /**
   * Describes the cases of `enumType`.
   *
   * @throws dev.coilix.config.enums.InvalidEnumException
   *   if `enumType` is not an annotated enum, or two of its cases share a name or alias
   */
  def resolve(enumType: Class[?]): EnumDefinition
}

/**
 * Provides the default [[dev.coilix.config.enums.EnumResolver]].
 *
 * The resolver is needed while the configuration itself is read, so it cannot be replaced through a `class` key.
 */
object EnumResolver {

  /** The resolver that reads Scala 3 enums by reflection. */
  def default: EnumResolver =
    ReflectiveEnumResolver()
}
