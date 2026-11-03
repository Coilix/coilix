package dev.coilix.context

import dev.coilix.annotation.Slf4s
import dev.coilix.common.ClassTags.runtimeTypeOf
import java.util.concurrent.atomic.AtomicBoolean
import scala.reflect.ClassTag

/**
 * An application context backed by a [[dev.coilix.context.BeanRegistry]].
 *
 * The registry is passed by name, so the context can exist before its beans and be injected into them. Closing the context
 * logs a message at `INFO` level.
 *
 * @param registry
 *   the beans of the context, evaluated on first access
 */
@Slf4s
final class DefaultApplicationContext(registry: => BeanRegistry) extends ConfigurableApplicationContext {

  private val active = AtomicBoolean(true)

  override def getBean[T: ClassTag]: T =
    getBeanOption[T].getOrElse(throw NoSuchBeanException[T])

  override def getBeanOption[T: ClassTag]: Option[T] =
    registry.find(runtimeTypeOf[T]).map(bean => runtimeTypeOf[T].cast(bean.instance))

  override def getBeansOfType[T: ClassTag]: List[T] =
    registry.findAll(runtimeTypeOf[T]).map(bean => runtimeTypeOf[T].cast(bean.instance))

  override def getBeanByName(beanName: String): Option[AnyRef] =
    registry.findByName(beanName).map(_.instance)

  override def beanNames: List[String] =
    registry.beans.map(_.beanName)

  override def isActive: Boolean = active.get

  override def close(): Unit =
    if (active.compareAndSet(true, false)) log.info("Closing application context")
}
