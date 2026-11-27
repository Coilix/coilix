package dev.coilix.concurrency

import dev.coilix.context.CoilixModule
import dev.coilix.context.DefinitionProcessor
import dev.coilix.context.EffectInterpreter

/**
 * Adds the cats-effect runtime and the [[dev.coilix.concurrency.TaskExecutor]] and [[dev.coilix.concurrency.BlockingExecutor]]
 * beans to every application, runs [[dev.coilix.annotation.Scheduled]] methods on fibers and creates beans with
 * [[dev.coilix.annotation.Async]] methods as proxies.
 *
 * `IO` is the effect of the application: a `@Bean` method may return `IO[A]` or `Resource[IO, A]`, which the
 * [[dev.coilix.concurrency.EffectfulBeanProcessor]] checks and the [[dev.coilix.concurrency.IOEffectInterpreter]] creates.
 *
 * The module is part of the core and is on the classpath of `coilix-boot`. It can be disabled with
 * `coilix.modules.concurrency.enabled = false`.
 */
final class ConcurrencyModule extends CoilixModule {

  override def name: String = "concurrency"

  override def components: List[Class[?]] = List(classOf[ConcurrencyConfiguration], classOf[CatsEffectConfiguration])

  override def definitionProcessors: List[DefinitionProcessor] = List(AsyncDefinitionProcessor(), EffectfulBeanProcessor())

  override def effectInterpreter: Option[EffectInterpreter] = Some(IOEffectInterpreter())
}
