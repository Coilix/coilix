package dev.coilix.concurrency

import dev.coilix.context.CoilixModule
import dev.coilix.context.DefinitionProcessor

/**
 * Adds the [[dev.coilix.concurrency.TaskExecutor]] and [[dev.coilix.concurrency.TaskScheduler]] beans to every application,
 * runs [[dev.coilix.annotation.Scheduled]] methods and creates beans with [[dev.coilix.annotation.Async]] methods as proxies.
 *
 * The module is part of the core and is on the classpath of `coilix-boot`. It can be disabled with
 * `coilix.modules.concurrency.enabled = false`.
 */
final class ConcurrencyModule extends CoilixModule {

  override def name: String = "concurrency"

  override def components: List[Class[?]] = List(classOf[ConcurrencyConfiguration])

  override def definitionProcessors: List[DefinitionProcessor] = List(AsyncDefinitionProcessor())
}
