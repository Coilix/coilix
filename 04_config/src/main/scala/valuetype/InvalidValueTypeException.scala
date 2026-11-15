package dev.coilix.config
package valuetype

import dev.coilix.common.CoilixException

/**
 * Thrown when a class annotated with [[dev.coilix.annotation.ValueType]] cannot be used as a value type, for example
 * because its constructor has more than one parameter.
 *
 * The message names the class and says what is missing.
 */
final class InvalidValueTypeException(message: String) extends CoilixException(message)
