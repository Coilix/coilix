package dev.coilix.concurrency

import dev.coilix.context.BeanCreationException
import dev.coilix.context.InstanceFactory
import dev.coilix.context.ProxiedBean
import java.lang.reflect.Proxy

/**
 * Creates a bean with the original factory and returns it wrapped in a proxy that runs its [[dev.coilix.annotation.Async]]
 * methods through an [[dev.coilix.concurrency.AsyncInvocationHandler]].
 *
 * The [[dev.coilix.concurrency.BlockingExecutor]] and the [[dev.coilix.concurrency.AsyncTasks]] are the last arguments, after
 * the arguments of the original factory.
 *
 * @param original
 *   creates the bean behind the proxy
 * @param interfaces
 *   the traits the proxy implements
 * @param asyncMethods
 *   the asynchronous methods by name and parameter types
 */
case class AsyncProxyFactory(
  original:     InstanceFactory,
  interfaces:   List[Class[?]],
  asyncMethods: Map[(String, List[Class[?]]), AsyncMethod]
) extends InstanceFactory {

  override def create(arguments: List[AnyRef]): AnyRef =
    arguments.takeRight(2) match {
      case List(blocking: BlockingExecutor, tasks: AsyncTasks) =>
        proxy(original.create(arguments.dropRight(2)), blocking, tasks)
      case _                                                   =>
        throw BeanCreationException("An @Async bean was created without the BlockingExecutor and AsyncTasks arguments")
    }

  private def proxy(target: AnyRef, blocking: BlockingExecutor, tasks: AsyncTasks): AnyRef =
    Proxy.newProxyInstance(
      target.getClass.getClassLoader,
      (interfaces :+ classOf[ProxiedBean]).toArray,
      AsyncInvocationHandler(target, blocking, tasks, asyncMethods)
    )
}
