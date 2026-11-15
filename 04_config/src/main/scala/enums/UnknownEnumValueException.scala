package dev.coilix.config
package enums

import dev.coilix.common.CoilixException

/**
 * Thrown when a name does not match any case of an [[dev.coilix.annotation.Enumerated]] enum.
 *
 * The message names the enum, the unknown name and the allowed names.
 */
final class UnknownEnumValueException(message: String) extends CoilixException(message)
