package dev.coilix.context

import dev.coilix.annotation.Slf4s
import java.lang.reflect.InvocationTargetException
import java.lang.reflect.Method
import scala.util.Failure
import scala.util.Try

/**
 * Invokes the `@PreDestroy` methods of each bean, starting with the bean created last.
 *
 * An exception thrown by a method is logged at `WARN` level with the bean and method name, and the remaining methods and beans
 * are still destroyed. For a [[dev.coilix.context.ProxiedBean]] the methods run on the instance behind the proxy. This is the
 * default [[dev.coilix.context.BeanDestroyer]].
 */
@Slf4s
final class PreDestroyInvoker extends BeanDestroyer {

  override def destroy(beans: List[RegisteredBean]): Unit =
    beans.reverse.foreach(destroyBean)

  private def destroyBean(bean: RegisteredBean): Unit =
    bean.lifecycle.destroy.foreach(invoke(bean))

  private def invoke(bean: RegisteredBean)(method: Method): Unit =
    Try(method.invoke(ProxiedBean.unwrap(bean.instance))) match {
      case Failure(failure) => log.warn(s"@PreDestroy method ${method.getName} of bean '${bean.beanName}' failed", causeOf(failure))
      case _                => ()
    }

  private def causeOf(failure: Throwable): Throwable =
    failure match {
      case invocation: InvocationTargetException => invocation.getCause
      case other => other
    }
}
