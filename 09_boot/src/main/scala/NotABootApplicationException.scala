package dev.coilix.boot

import dev.coilix.common.CoilixException

/**
 * Thrown when the class passed to `Coilix.run` is not annotated with `@CoilixApplication`.
 *
 * @param applicationClass
 *   the class passed to `Coilix.run`
 */
final class NotABootApplicationException(applicationClass: Class[?])
    extends CoilixException(s"${applicationClass.getName} is not annotated with @CoilixApplication")
