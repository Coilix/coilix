package dev.coilix.common

/**
 * Base class of all exceptions thrown by Coilix.
 *
 * Catching `CoilixException` handles every framework error, such as a missing bean or an invalid configuration, without
 * catching unrelated runtime exceptions.
 */
class CoilixException(message: String) extends RuntimeException(message)
