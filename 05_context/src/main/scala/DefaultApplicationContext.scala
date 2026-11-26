package dev.coilix.context

import cats.effect.IO
import cats.syntax.all.*
import dev.coilix.annotation.Slf4s
import dev.coilix.common.TypeInfo
import java.util.concurrent.atomic.AtomicBoolean

/**
 * An application context backed by a [[dev.coilix.context.BeanRegistry]].
 *
 * The registry is passed by name, so the context can exist before its beans and be injected into them. Closing the context
 * logs a message at `INFO` level, runs [[dev.coilix.context.ContextLifecycleListener.onContextClosing]] of the listener beans
 * phase by phase in the order of [[dev.coilix.context.ClosingPhase]], and then destroys the beans with the
 * [[dev.coilix.context.BeanDestroyer]], after `release`, which closes the beans opened by an
 * [[dev.coilix.context.EffectInterpreter]]. The beans are destroyed also when a listener or `release` fails. `close()` runs the
 * listeners and `release` with [[dev.coilix.context.SynchronousLifecycle.run]] and destroys the beans on the calling thread, so
 * an `IORuntime` bean is not shut down while it runs them.
 *
 * @param registry
 *   the beans of the context, evaluated on each access
 * @param destroyer
 *   destroys the beans when the context closes
 * @param release
 *   closes the beans opened by an effect interpreter, once, before the other beans are destroyed
 */
@Slf4s
final class DefaultApplicationContext(
  registry: => BeanRegistry,
  destroyer: BeanDestroyer = PreDestroyInvoker(),
  release:   IO[Unit]      = IO.unit
) extends ConfigurableApplicationContext {

  private val active = AtomicBoolean(true)

  override def getBean[T: TypeInfo]: T =
    getBeanOption[T].getOrElse(throw NoSuchBeanException[T])

  override def getBeanOption[T: TypeInfo]: Option[T] =
    registry.find(TypeInfo[T].genericType).map(bean => TypeInfo[T].runtimeClass.cast(bean.instance))

  override def getBeansOfType[T: TypeInfo]: List[T] =
    registry.findAll(TypeInfo[T].genericType).map(bean => TypeInfo[T].runtimeClass.cast(bean.instance))

  override def getBeanByName(beanName: String): Option[AnyRef] =
    registry.findByName(beanName).map(_.instance)

  override def beanNames: List[String] =
    registry.beanNames

  override def createdBeans: List[RegisteredBean] =
    registry.beans

  override def isActive: Boolean = active.get

  override def start: IO[Unit] =
    listeners.traverse_(_.onContextStarted(this))

  override def shutdown: IO[Unit] =
    IO(active.compareAndSet(true, false)).ifM(closing.guarantee(IO.blocking(destroyed())), IO.unit)

  override def close(): Unit =
    if (active.compareAndSet(true, false)) {
      try SynchronousLifecycle.run(this, closing)
      finally destroyed()
    }

  private def closing: IO[Unit] = closingListeners *> release

  private def destroyed(): Unit = destroyer.destroy(registry.beans)

  private def closingListeners: IO[Unit] = {
    val reversed = listeners.reverse
    logF.info("Closing application context") *>
      ClosingPhase.values.toList.traverse_(phase => reversed.filter(_.closingPhase == phase).traverse_(notifyClosing))
  }

  private def listeners: List[ContextLifecycleListener] =
    createdBeans.map(_.instance).collect { case listener: ContextLifecycleListener => listener }

  private def notifyClosing(listener: ContextLifecycleListener): IO[Unit] =
    listener.onContextClosing.handleErrorWith(failure =>
      logF.warn(s"Context listener ${listener.getClass.getName} failed while the context was closing", failure)
    )
}
