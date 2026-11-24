package dev.coilix.catseffect

import cats.effect.IO
import cats.effect.Resource
import cats.syntax.foldable.*
import cats.syntax.parallel.*
import dev.coilix.annotation.Slf4s
import dev.coilix.context.BeanCreationException
import dev.coilix.context.BeanDefinition
import dev.coilix.context.BeanDestroyer
import dev.coilix.context.BeanInstantiator
import dev.coilix.context.BeanRegistry
import dev.coilix.context.BeanResult
import dev.coilix.context.DeferredBean
import dev.coilix.context.ProxiedBean
import dev.coilix.context.RegisteredBean
import java.lang.reflect.InvocationTargetException
import java.lang.reflect.Method

/**
 * Describes the creation of beans as a `Resource[IO, BeanRegistry]` for the [[dev.coilix.catseffect.IOEffectInterpreter]].
 *
 * The beans of one level are opened at the same time, each level after the previous one, and released in the reverse order of
 * levels. A plain bean is created in `IO.blocking` and its `@PreDestroy` methods run when it is released. A bean made by an
 * `IO[A]` gets the value of the effect, and one made by a `Resource[IO, A]` the value of the opened resource, which is closed
 * after its `@PreDestroy` methods. A lazy or prototype plain bean is registered to be created on lookup, as without effects.
 *
 * `@PostConstruct` methods that return `IO[Unit]` run after those that return `Unit`, and a failure of one stops the startup with
 * a [[dev.coilix.context.BeanCreationException]]. `@PreDestroy` methods that return `IO[Unit]` run before those that return
 * `Unit`, and a failure of one is logged at `WARN` level without stopping the release.
 *
 * @param instantiator
 *   calls the factories and completes the beans
 * @param destroyer
 *   runs the `@PreDestroy` methods of a bean that return `Unit`
 * @param timeouts
 *   limits each finalizer
 */
@Slf4s
final class IOBeanResources(instantiator: BeanInstantiator, destroyer: BeanDestroyer, timeouts: ShutdownTimeouts) {

  /** Opens the beans of `levels` on top of `registry` and gives the registry with all of them. */
  def levels(levels: List[List[BeanDefinition]], registry: BeanRegistry): Resource[IO, BeanRegistry] =
    levels.foldLeft(Resource.pure[IO, BeanRegistry](registry))((opened, definitions) => opened.flatMap(level(definitions)))

  private def level(definitions: List[BeanDefinition])(registry: BeanRegistry): Resource[IO, BeanRegistry] =
    definitions.parTraverse(entry(registry)).map(_.foldLeft(registry)((current, register) => register(current)))

  private def entry(registry: BeanRegistry)(definition: BeanDefinition): Resource[IO, BeanRegistry => BeanRegistry] =
    definition.result match {
      case BeanResult.Plain if !definition.creation.atStartup => Resource.pure(deferred(definition))
      case BeanResult.Plain                                   => destroyable(definition, IO.blocking(instantiator.instantiate(definition, registry)))
      case BeanResult.Deferred(_, _)                          => completed(definition, Resource.eval(produced(definition, registry).flatMap(run)))
      case BeanResult.Managed(_, _)                           =>
        completed(definition, Resource.eval(produced(definition, registry)).flatMap(open(definition)))
    }

  private def deferred(definition: BeanDefinition)(registry: BeanRegistry): BeanRegistry =
    registry.defer(DeferredBean.of(definition)(lookup => withoutEffects(instantiator.instantiate(definition, lookup))))

  private def withoutEffects(bean: RegisteredBean): RegisteredBean =
    bean.copy(lifecycle = bean.lifecycle.withoutEffects(bean.beanName))

  private def produced(definition: BeanDefinition, registry: BeanRegistry): IO[AnyRef] =
    IO.blocking(instantiator.produce(definition, registry))

  private def run(effect: AnyRef): IO[AnyRef] =
    effect.asInstanceOf[IO[AnyRef]]

  private def open(definition: BeanDefinition)(resource: AnyRef): Resource[IO, AnyRef] =
    Resource
      .make(resource.asInstanceOf[Resource[IO, AnyRef]].allocated)((_, release) => timeouts.bounded(definition, release))
      .map(_._1)

  private def completed(definition: BeanDefinition, value: Resource[IO, AnyRef]): Resource[IO, BeanRegistry => BeanRegistry] =
    value.flatMap(instance => destroyable(definition, IO.blocking(instantiator.complete(definition, instance))))

  private def destroyable(definition: BeanDefinition, create: IO[RegisteredBean]): Resource[IO, BeanRegistry => BeanRegistry] =
    Resource
      .make(create.flatTap(initialized))(bean => timeouts.bounded(definition, destroyed(bean)))
      .map(bean => _.register(bean.copy(lifecycle = bean.lifecycle.copy(destroy = Nil))))

  private def initialized(bean: RegisteredBean): IO[Unit] =
    bean.lifecycle.effectfulInit.traverse_(method => effectOf(bean, method).adaptError(initFailed(bean, method)))

  private def initFailed(bean: RegisteredBean, method: Method): PartialFunction[Throwable, Throwable] = {
    case failure =>
      BeanCreationException(s"@PostConstruct method ${method.getName} of bean '${bean.beanName}' failed: $failure", Some(failure))
  }

  private def destroyed(bean: RegisteredBean): IO[Unit] =
    bean.lifecycle.effectfulDestroy.traverse_(destroyedBy(bean)) *> IO.blocking(destroyer.destroy(List(bean)))

  private def destroyedBy(bean: RegisteredBean)(method: Method): IO[Unit] =
    effectOf(bean, method).handleError(failure =>
      log.warn(s"@PreDestroy method ${method.getName} of bean '${bean.beanName}' failed", failure)
    )

  private def effectOf(bean: RegisteredBean, method: Method): IO[Unit] =
    IO.blocking(invoked(bean, method)).flatten

  private def invoked(bean: RegisteredBean, method: Method): IO[Unit] =
    try {
      method.invoke(ProxiedBean.unwrap(bean.instance)).asInstanceOf[IO[Unit]]
    } catch {
      case failure: InvocationTargetException => throw failure.getCause
    }
}
