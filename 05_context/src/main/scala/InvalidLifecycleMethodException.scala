package dev.coilix.context

import dev.coilix.common.CoilixException

/**
 * Thrown while bean definitions are created when a `@PostConstruct` or `@PreDestroy` method has a wrong signature. The message
 * names the bean and the method.
 */
final class InvalidLifecycleMethodException(message: String) extends CoilixException(message)
