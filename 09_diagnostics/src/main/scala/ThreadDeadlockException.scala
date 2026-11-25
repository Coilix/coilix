package dev.coilix.diagnostics

import dev.coilix.common.CoilixException

/**
 * Handed to the [[dev.coilix.concurrency.FatalErrorHandler]] when the diagnostics monitor finds deadlocked threads and
 * `coilix.diagnostics.threads.on-deadlock` is `shutdown`.
 *
 * @param description
 *   the message of the deadlock finding, with the names of the threads
 */
final class ThreadDeadlockException(description: String) extends CoilixException(description)
