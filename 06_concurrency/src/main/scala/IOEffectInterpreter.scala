package dev.coilix.concurrency

import cats.effect.unsafe.IORuntime
import dev.coilix.common.ClassTags.runtimeTypeOf
import dev.coilix.common.GenericType
import dev.coilix.config.Environment
import dev.coilix.context.BeanDefinition
import dev.coilix.context.BeanDestroyer
import dev.coilix.context.BeanInstantiator
import dev.coilix.context.BeanRegistry
import dev.coilix.context.EffectInterpreter
import dev.coilix.context.InterpretedBeans
import dev.coilix.context.NoSuchBeanException
import scala.reflect.ClassTag

/**
 * Creates beans made by `IO[A]` and `Resource[IO, A]`, given to the context by the [[dev.coilix.concurrency.ConcurrencyModule]].
 *
 * The remaining levels of the plan become one `Resource[IO, BeanRegistry]` built by [[dev.coilix.concurrency.IOBeanResources]],
 * which is opened with a single `unsafeRunSync` on the `ioRuntime` bean. Closing the context releases it with another one,
 * waiting for each finalizer at most `coilix.cats-effect.shutdown-timeout` or the limit of
 * [[dev.coilix.annotation.ShutdownTimeout]]. When a bean fails to open, the beans opened before it are released and the failure
 * stops the startup.
 */
final class IOEffectInterpreter extends EffectInterpreter {

  override def interpret(
    levels:       List[List[BeanDefinition]],
    registry:     BeanRegistry,
    instantiator: BeanInstantiator,
    destroyer:    BeanDestroyer
  ): InterpretedBeans = {
    given runtime: IORuntime = required[IORuntime](registry)
    val timeouts          =
      ShutdownTimeouts(found[CatsEffectSettings](registry).getOrElse(CatsEffectSettings()), found[Environment](registry))
    val (opened, release) = IOBeanResources(instantiator, destroyer, timeouts).levels(levels, registry).allocated.unsafeRunSync()
    InterpretedBeans(opened, () => release.unsafeRunSync())
  }

  private def required[T <: AnyRef: ClassTag](registry: BeanRegistry): T =
    found[T](registry).getOrElse(throw new NoSuchBeanException(s"No bean of type ${runtimeTypeOf[T].getName} to run effects on"))

  private def found[T <: AnyRef: ClassTag](registry: BeanRegistry): Option[T] =
    registry.find(GenericType.of(runtimeTypeOf[T])).map(_.instance).collect { case instance: T => instance }
}
