package dev.coilix.config
package enums

import dev.coilix.common.CoilixException

/**
 * Thrown when a class annotated with [[dev.coilix.annotation.Enumerated]] cannot be used as an enum, for example because it
 * is not an enum or two cases have the same name.
 *
 * The message names the class and the problem.
 */
final class InvalidEnumException(message: String) extends CoilixException(message)
