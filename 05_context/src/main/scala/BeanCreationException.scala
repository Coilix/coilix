package dev.coilix.context

import dev.coilix.common.CoilixException

/**
 * Thrown when a bean cannot be created, for example when a `@Bean` method returns `null` or a `@PostConstruct` method throws.
 *
 * @param cause
 *   the exception that stopped the creation, when there is one
 */
final class BeanCreationException(message: String, cause: Option[Throwable] = None) extends CoilixException(message) {
  cause.foreach(initCause)
}
