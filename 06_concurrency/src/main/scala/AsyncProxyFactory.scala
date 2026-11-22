package dev.coilix.concurrency

import dev.coilix.context.BeanCreationException
import dev.coilix.context.InstanceFactory
import dev.coilix.context.ProxiedBean
import java.lang.reflect.Proxy

/**
 * Creates a bean with the original factory and returns it wrapped in a proxy that runs its
 * [[dev.coilix.annotation.Async]] methods on the [[dev.coilix.concurrency.TaskExecutor]] or the
 * [[dev.coilix.concurrency.BlockingExecutor]].
 *
 * The two executors are the last arguments, after the arguments of the original factory.
 *
 * @param original
 *   creates the bean behind the proxy
 * @param interfaces
 *   the traits the proxy implements
 * @param asyncMethods
 *   whether each asynchronous method, by name and parameter types, blocks
 */
final class AsyncProxyFactory(
  original:     InstanceFactory,
  interfaces:   List[Class[?]],
  asyncMethods: Map[(String, List[Class[?]]), Boolean]
) extends InstanceFactory {

  override def create(arguments: List[AnyRef]): AnyRef =
    arguments.takeRight(2) match {
      case List(compute: TaskExecutor, blocking: BlockingExecutor) =>
        proxy(original.create(arguments.dropRight(2)), compute, blocking)
      case _                                                       =>
        throw BeanCreationException("An @Async bean was created without the TaskExecutor and BlockingExecutor arguments")
    }

  private def proxy(target: AnyRef, compute: TaskExecutor, blocking: BlockingExecutor): AnyRef =
    Proxy.newProxyInstance(
      target.getClass.getClassLoader,
      (interfaces :+ classOf[ProxiedBean]).toArray,
      AsyncInvocationHandler(target, compute, blocking, asyncMethods)
    )
}
