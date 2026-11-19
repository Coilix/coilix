package dev.coilix.concurrency

import dev.coilix.context.BeanCreationException
import dev.coilix.context.InstanceFactory
import dev.coilix.context.ProxiedBean
import java.lang.reflect.Proxy

/**
 * Creates a bean with the original factory and returns it wrapped in a proxy that runs its
 * [[dev.coilix.annotation.Async]] methods on the [[dev.coilix.concurrency.TaskExecutor]].
 *
 * The executor is the last argument, after the arguments of the original factory.
 *
 * @param original
 *   creates the bean behind the proxy
 * @param interfaces
 *   the traits the proxy implements
 * @param asyncMethods
 *   the name and parameter types of each asynchronous method
 */
final class AsyncProxyFactory(
  original:     InstanceFactory,
  interfaces:   List[Class[?]],
  asyncMethods: Set[(String, List[Class[?]])]
) extends InstanceFactory {

  override def create(arguments: List[AnyRef]): AnyRef =
    arguments.lastOption match {
      case Some(executor: TaskExecutor) => proxy(original.create(arguments.dropRight(1)), executor)
      case _                            => throw BeanCreationException("An @Async bean was created without a TaskExecutor argument")
    }

  private def proxy(target: AnyRef, executor: TaskExecutor): AnyRef =
    Proxy.newProxyInstance(
      target.getClass.getClassLoader,
      (interfaces :+ classOf[ProxiedBean]).toArray,
      AsyncInvocationHandler(target, executor, asyncMethods)
    )
}
