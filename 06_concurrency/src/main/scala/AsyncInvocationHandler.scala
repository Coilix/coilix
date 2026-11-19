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
 * A call of an asynchronous method runs on `executor`: a `Future` result is taken from the run, and a `Unit` method is started
 * without waiting. Every other call goes directly to `target` on the calling thread.
 *
 * @param target
 *   the bean behind the proxy
 * @param executor
 *   runs the asynchronous methods
 * @param asyncMethods
 *   the name and parameter types of each asynchronous method
 */
final class AsyncInvocationHandler(target: AnyRef, executor: TaskExecutor, asyncMethods: Set[(String, List[Class[?]])])
    extends InvocationHandler {

  override def invoke(proxy: AnyRef, method: Method, arguments: Array[AnyRef]): AnyRef = {
    val values = Option(arguments).fold(List.empty[AnyRef])(_.toList)
    if (method.getDeclaringClass == classOf[ProxiedBean]) target
    else if (!asyncMethods.contains((method.getName, method.getParameterTypes.toList))) call(method, values)
    else if (classOf[Future[?]].isAssignableFrom(method.getReturnType)) submitted(method, values)
    else started(method, values)
  }

  private def submitted(method: Method, values: List[AnyRef]): Future[Any] =
    executor
      .submit(call(method, values))
      .flatMap {
        case future: Future[?] => future
        case other => Future.successful(other)
      }(using ExecutionContext.parasitic)

  private def started(method: Method, values: List[AnyRef]): AnyRef = {
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
