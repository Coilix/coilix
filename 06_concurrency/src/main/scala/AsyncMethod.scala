package dev.coilix.concurrency

/**
 * How the proxy of a bean runs one of its [[dev.coilix.annotation.Async]] methods.
 *
 * @param label
 *   names the method and the bean in logs
 * @param blocking
 *   whether the method runs on the [[dev.coilix.concurrency.BlockingExecutor]] instead of the compute pool
 * @param detached
 *   whether the method returns `IO[Unit]`, so its effect is started on a fiber and the caller does not wait for it
 */
case class AsyncMethod(label: String, blocking: Boolean, detached: Boolean)
