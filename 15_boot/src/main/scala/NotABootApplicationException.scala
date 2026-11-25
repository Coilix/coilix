package dev.coilix.boot

import dev.coilix.common.CoilixException

/**
 * Thrown when the class of an application is not annotated with `@CoilixApplication`.
 *
 * @param applicationClass
 *   the class of the application
 */
final class NotABootApplicationException(applicationClass: Class[?])
    extends CoilixException(s"${applicationClass.getName} is not annotated with @CoilixApplication")
