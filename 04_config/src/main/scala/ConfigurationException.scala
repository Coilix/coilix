package dev.coilix.config

import dev.coilix.common.CoilixException

/**
 * Thrown when the configuration is missing a value, has a value of the wrong type, or names a class that cannot be used.
 *
 * The message names the configuration path and says what was expected.
 */
final class ConfigurationException(message: String) extends CoilixException(message)
