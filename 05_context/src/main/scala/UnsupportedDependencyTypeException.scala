package dev.coilix.context

import dev.coilix.common.CoilixException

/**
 * Thrown when the type of a constructor or `@Bean` method parameter cannot be matched reliably, such as a type with an unbound
 * type variable or a type lambda that the JVM keeps only as its class.
 *
 * The message names the parameter, the method or constructor that declares it, and the reason.
 */
final class UnsupportedDependencyTypeException(message: String) extends CoilixException(message)
