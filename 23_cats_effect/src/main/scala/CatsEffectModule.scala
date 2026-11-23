package dev.coilix.catseffect

import dev.coilix.context.CoilixModule

/**
 * Adds a cats-effect `IORuntime` to the application, whose compute pool becomes the only compute pool: the
 * [[dev.coilix.concurrency.TaskExecutor]] runs on it, so the default pool of the concurrency module is not created.
 *
 * The module is found on the classpath of `coilix-cats-effect` and can be disabled with
 * `coilix.modules.cats-effect.enabled = false`.
 */
final class CatsEffectModule extends CoilixModule {

  override def name: String = "cats-effect"

  override def components: List[Class[?]] = List(classOf[CatsEffectConfiguration])
}
