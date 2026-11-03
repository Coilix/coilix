package dev.coilix.context

import dev.coilix.common.CoilixException

/** Thrown when a bean cannot be created, for example when a `@Bean` method returns `null`. */
final class BeanCreationException(message: String) extends CoilixException(message)
