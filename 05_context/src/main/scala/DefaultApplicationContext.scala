package dev.coilix.context

import dev.coilix.annotation.Slf4s
import dev.coilix.common.TypeInfo
import java.util.concurrent.atomic.AtomicBoolean
import scala.util.Try

/**
 * An application context backed by a [[dev.coilix.context.BeanRegistry]].
 *
 * The registry is passed by name, so the context can exist before its beans and be injected into them. Closing the context
 * logs a message at `INFO` level, calls [[dev.coilix.context.ContextLifecycleListener.onContextClosing]] on the listener
 * beans and destroys the beans with the [[dev.coilix.context.BeanDestroyer]].
 *
 * @param registry
 *   the beans of the context, evaluated on first access
 * @param destroyer
 *   destroys the beans when the context closes
 */
@Slf4s
final class DefaultApplicationContext(registry: => BeanRegistry, destroyer: BeanDestroyer = PreDestroyInvoker())
    extends ConfigurableApplicationContext {

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
    registry.beans.map(_.beanName)

  override def isActive: Boolean = active.get

  override def close(): Unit =
    if (active.compareAndSet(true, false)) destroyBeans()

  private def destroyBeans(): Unit = {
    log.info("Closing application context")
    getBeansOfType[ContextLifecycleListener].reverse.foreach(notifyClosing)
    destroyer.destroy(registry.beans)
  }

  private def notifyClosing(listener: ContextLifecycleListener): Unit =
    Try(listener.onContextClosing()).failed.foreach(failure =>
      log.warn(s"Context listener ${listener.getClass.getName} failed while the context was closing", failure)
    )
}
