package dev.coilix.catseffect

import dev.coilix.context.CoilixModule
import dev.coilix.context.DefinitionProcessor
import dev.coilix.context.EffectInterpreter

/**
 * Adds a cats-effect `IORuntime` to the application, whose compute pool becomes the only compute pool: the
 * [[dev.coilix.concurrency.TaskExecutor]] runs on it, so the default pool of the concurrency module is not created.
 *
 * The module is found on the classpath of `coilix-cats-effect` and can be disabled with
 * `coilix.modules.cats-effect.enabled = false`.
 *
 * A `@Bean` method may return `IO[A]` or `Resource[IO, A]`: the module checks such beans with the
 * [[dev.coilix.catseffect.EffectfulBeanProcessor]] and creates them with the [[dev.coilix.catseffect.IOEffectInterpreter]], and
 * other beans receive the value `A`.
 */
final class CatsEffectModule extends CoilixModule {

  override def name: String = "cats-effect"

  override def components: List[Class[?]] = List(classOf[CatsEffectConfiguration])

  override def definitionProcessors: List[DefinitionProcessor] = List(EffectfulBeanProcessor())

  override def effectInterpreter: Option[EffectInterpreter] = Some(IOEffectInterpreter())
}
