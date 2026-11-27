package dev.coilix.concurrency

import cats.effect.IO
import dev.coilix.context.ProxiedBean
import java.lang.reflect.InvocationHandler
import java.lang.reflect.InvocationTargetException
import java.lang.reflect.Method

/**
 * Handles calls to the proxy of a bean with [[dev.coilix.annotation.Async]] methods.
 *
 * A call of an asynchronous method returns an `IO` that calls the bean when it runs: on the compute pool, or on `blocking` for a
 * method marked as blocking, so also the code that builds the effect of the method runs there. A method returning `IO[Unit]`
 * is started on a fiber of `tasks` and the `IO` of the call ends at once. Every other call goes directly to `target` on the
 * calling thread.
 *
 * @param target
 *   the bean behind the proxy
 * @param blocking
 *   runs the asynchronous methods that block
 * @param tasks
 *   keeps the fibers of the methods that run without the caller waiting
 * @param asyncMethods
 *   the asynchronous methods by name and parameter types
 */
case class AsyncInvocationHandler(
  target:       AnyRef,
  blocking:     BlockingExecutor,
  tasks:        AsyncTasks,
  asyncMethods: Map[(String, List[Class[?]]), AsyncMethod]
) extends InvocationHandler {

  override def invoke(proxy: AnyRef, method: Method, arguments: Array[AnyRef]): AnyRef = {
    val values = Option(arguments).fold(List.empty[AnyRef])(_.toList)
    if (method.getDeclaringClass == classOf[ProxiedBean]) target
    else {
      asyncMethods.get((method.getName, method.getParameterTypes.toList)) match {
        case None        => call(method, values)
        case Some(async) => dispatched(async, placed(async, IO.defer(effect(method, values))))
      }
    }
  }

  private def placed(async: AsyncMethod, run: IO[Any]): IO[Any] =
    if (async.blocking) run.evalOn(blocking.executionContext) else IO.cede *> run

  private def dispatched(async: AsyncMethod, run: IO[Any]): IO[Any] =
    if (async.detached) tasks.start(async.label, run.void) else run

  /** The effect returned by the bean. The cast cannot fail, because only methods that return `IO` are asynchronous. */
  @SuppressWarnings(Array("org.wartremover.warts.AsInstanceOf"))
  private def effect(method: Method, values: List[AnyRef]): IO[Any] =
    call(method, values).asInstanceOf[IO[Any]]

  private def call(method: Method, values: List[AnyRef]): AnyRef =
    try {
      method.invoke(target, values*)
    } catch {
      case failure: InvocationTargetException => throw Option(failure.getCause).getOrElse(failure)
    }
}
