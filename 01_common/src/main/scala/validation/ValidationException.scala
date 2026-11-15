package dev.coilix.common
package validation

import dev.coilix.common.CoilixException

/**
 * Thrown when a value breaks one or more validation rules.
 *
 * @param violations
 *   every broken rule, in the order the validator checks them
 */
final class ValidationException(val violations: List[Violation]) extends CoilixException(ValidationException.message(violations))

/** Builds the message of [[dev.coilix.common.validation.ValidationException]]. */
object ValidationException {

  private def message(violations: List[Violation]): String =
    violations.mkString("Validation failed: ", "; ", "")
}
