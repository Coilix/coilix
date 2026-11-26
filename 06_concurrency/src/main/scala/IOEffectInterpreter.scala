package dev.coilix.concurrency

import cats.effect.IO
import cats.effect.Resource
import dev.coilix.common.ClassTags.runtimeTypeOf
import dev.coilix.common.GenericType
import dev.coilix.config.Environment
import dev.coilix.context.BeanDefinition
import dev.coilix.context.BeanDestroyer
import dev.coilix.context.BeanInstantiator
import dev.coilix.context.BeanRegistry
import dev.coilix.context.EffectInterpreter
import scala.reflect.ClassTag

/**
 * Creates beans made by `IO[A]` and `Resource[IO, A]`, given to the context by the [[dev.coilix.concurrency.ConcurrencyModule]].
 *
 * The remaining levels of the plan become one `Resource[IO, BeanRegistry]` built by [[dev.coilix.concurrency.IOBeanResources]],
 * opened by the fiber that creates the context. Closing the context releases it, waiting for each finalizer at most
 * `coilix.cats-effect.shutdown-timeout` or the limit of [[dev.coilix.annotation.ShutdownTimeout]]. When a bean fails to open,
 * the beans opened before it are released and the failure stops the startup.
 */
final class IOEffectInterpreter extends EffectInterpreter {

  override def interpret(
    levels:       List[List[BeanDefinition]],
    registry:     BeanRegistry,
    instantiator: BeanInstantiator,
    destroyer:    BeanDestroyer
  ): Resource[IO, BeanRegistry] = {
    val timeouts =
      ShutdownTimeouts(found[CatsEffectSettings](registry).getOrElse(CatsEffectSettings()), found[Environment](registry))
    IOBeanResources(instantiator, destroyer, timeouts).levels(levels, registry)
  }

  private def found[T <: AnyRef: ClassTag](registry: BeanRegistry): Option[T] =
    registry.find(GenericType.of(runtimeTypeOf[T])).map(_.instance).collect { case instance: T => instance }
}
