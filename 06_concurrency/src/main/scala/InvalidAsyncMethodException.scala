package dev.coilix.concurrency

import dev.coilix.common.CoilixException

/**
 * Thrown at startup when a method annotated with [[dev.coilix.annotation.Async]] cannot run asynchronously, for example
 * because no trait of the bean declares it or it returns neither `Unit` nor `Future`. The message names the bean and the method.
 */
final class InvalidAsyncMethodException(message: String) extends CoilixException(message)
