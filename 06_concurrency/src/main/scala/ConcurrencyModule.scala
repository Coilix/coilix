package dev.coilix.concurrency

import dev.coilix.context.CoilixModule

/**
 * Adds the [[dev.coilix.concurrency.TaskExecutor]] and [[dev.coilix.concurrency.TaskScheduler]] beans to every application.
 *
 * The module is part of the core and is on the classpath of `coilix-boot`. It can be disabled with
 * `coilix.modules.concurrency.enabled = false`.
 */
final class ConcurrencyModule extends CoilixModule {

  override def name: String = "concurrency"

  override def components: List[Class[?]] = List(classOf[ConcurrencyConfiguration])
}
