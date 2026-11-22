package dev.coilix.concurrency

import dev.coilix.context.ProxiedBean
import java.lang.reflect.InvocationHandler
import java.lang.reflect.InvocationTargetException
import java.lang.reflect.Method
import scala.concurrent.ExecutionContext
import scala.concurrent.Future
import scala.runtime.BoxedUnit

/**
 * Handles calls to the proxy of a bean with [[dev.coilix.annotation.Async]] methods.
 *
 * A call of an asynchronous method runs on `compute`, or on `blocking` for a method marked as blocking: a `Future` result is taken from the run, and a `Unit` method is started
 * without waiting. Every other call goes directly to `target` on the calling thread.
 *
 * @param target
 *   the bean behind the proxy
 * @param compute
 *   runs the asynchronous methods that do not block
 * @param blocking
 *   runs the asynchronous methods that block
 * @param asyncMethods
 *   whether each asynchronous method, by name and parameter types, blocks
 */
final class AsyncInvocationHandler(
  target:       AnyRef,
  compute:      TaskExecutor,
  blocking:     BlockingExecutor,
  asyncMethods: Map[(String, List[Class[?]]), Boolean]
) extends InvocationHandler {

  override def invoke(proxy: AnyRef, method: Method, arguments: Array[AnyRef]): AnyRef = {
    val values = Option(arguments).fold(List.empty[AnyRef])(_.toList)
    if (method.getDeclaringClass == classOf[ProxiedBean]) target
    else {
      asyncMethods.get((method.getName, method.getParameterTypes.toList)) match {
        case None         => call(method, values)
        case Some(blocks) => dispatched(method, values, if (blocks) blocking else compute)
      }
    }
  }

  private def dispatched(method: Method, values: List[AnyRef], executor: ManagedExecutor): AnyRef =
    if (classOf[Future[?]].isAssignableFrom(method.getReturnType)) submitted(method, values, executor)
    else started(method, values, executor)

  private def submitted(method: Method, values: List[AnyRef], executor: ManagedExecutor): Future[Any] =
    executor
      .submit(call(method, values))
      .flatMap {
        case future: Future[?] => future
        case other => Future.successful(other)
      }(using ExecutionContext.parasitic)

  private def started(method: Method, values: List[AnyRef], executor: ManagedExecutor): AnyRef = {
    executor.execute {
      val _ = call(method, values)
    }
    BoxedUnit.UNIT
  }

  private def call(method: Method, values: List[AnyRef]): AnyRef =
    try {
      method.invoke(target, values*)
    } catch {
      case failure: InvocationTargetException => throw Option(failure.getCause).getOrElse(failure)
    }
}
