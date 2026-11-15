package dev.coilix.config
package valuetype

import dev.coilix.common.CoilixException

/**
 * Thrown when a [[dev.coilix.annotation.ValueType]] class rejects an inner value, for example in a `require` check.
 *
 * The message names the class and the value, and the exception thrown by the constructor is the cause.
 */
final class ValueCreationException(message: String, cause: Throwable) extends CoilixException(message) {
  initCause(cause)
}
