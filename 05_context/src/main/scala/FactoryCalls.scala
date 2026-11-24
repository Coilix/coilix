package dev.coilix.context

import java.lang.reflect.InvocationTargetException
import scala.util.control.NonFatal

/**
 * Calls a constructor or a `@Bean` method through reflection and reports what it throws as a
 * [[dev.coilix.context.BeanCreationException]] whose cause is the exception thrown by the code, not the
 * `InvocationTargetException` of reflection.
 *
 * A fatal error, such as `OutOfMemoryError`, is rethrown as it is, so the handling of fatal errors still sees it.
 */
private[context] object FactoryCalls {

  /** Runs the reflective call, describing the called code as `what` when it throws. */
  def guarded(what: => String)(call: => AnyRef): AnyRef =
    try {
      call
    } catch {
      case invocation: InvocationTargetException => throw failed(what, Option(invocation.getCause).getOrElse(invocation))
    }

  private def failed(what: String, cause: Throwable): Throwable =
    if (NonFatal(cause)) BeanCreationException(s"$what failed: ${describe(cause)}", Option(cause)) else cause

  private def describe(cause: Throwable): String =
    Option(cause.getMessage).fold(cause.getClass.getName)(message => s"${cause.getClass.getName}: $message")
}
