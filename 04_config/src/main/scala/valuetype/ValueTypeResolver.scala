package dev.coilix.config
package valuetype

/** Recognizes [[dev.coilix.annotation.ValueType]] classes and describes how to create them from their inner value. */
trait ValueTypeResolver {

  /** Whether `candidate` is a value type. */
  def isValueType(candidate: Class[?]): Boolean

  /**
   * Describes the value type `valueType`.
   *
   * @throws dev.coilix.config.valuetype.InvalidValueTypeException
   *   if `valueType` is not a value type or does not have the required constructor and accessor
   */
  def resolve(valueType: Class[?]): ValueTypeDefinition
}

/**
 * Provides the default [[dev.coilix.config.valuetype.ValueTypeResolver]].
 *
 * The resolver is needed while the configuration itself is read, so it cannot be replaced through a `class` key.
 */
object ValueTypeResolver {

  /** The resolver that reads the [[dev.coilix.annotation.ValueType]] annotation by reflection. */
  def default: ValueTypeResolver =
    ReflectiveValueTypeResolver()
}
