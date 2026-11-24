package dev.coilix.pekko

import dev.coilix.context.CoilixModule

/**
 * Adds a Pekko `ActorSystem` and a `Materializer` to the application, running on the executors of Coilix and stopped with the
 * context, as described in [[dev.coilix.pekko.CoilixActorSystem]].
 *
 * The module is found on the classpath of `coilix-pekko` and can be disabled with `coilix.modules.pekko.enabled = false`.
 */
final class PekkoModule extends CoilixModule {

  override def name: String = "pekko"

  override def components: List[Class[?]] = List(classOf[PekkoConfiguration])
}
