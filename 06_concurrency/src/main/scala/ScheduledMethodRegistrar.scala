package dev.coilix.concurrency

import cats.effect.FiberIO
import cats.effect.IO
import cats.effect.Ref
import cats.syntax.all.*
import dev.coilix.annotation.Scheduled
import dev.coilix.annotation.Slf4s
import dev.coilix.config.Environment
import dev.coilix.config.ValueExpression
import dev.coilix.context.ApplicationContext
import dev.coilix.context.ClosingPhase
import dev.coilix.context.ContextLifecycleListener
import dev.coilix.context.ProxiedBean
import java.lang.reflect.InvocationTargetException
import java.lang.reflect.Method
import java.lang.reflect.Modifier
import scala.concurrent.duration.FiniteDuration
import scala.util.control.NonFatal

/**
 * Runs the methods annotated with [[dev.coilix.annotation.Scheduled]], each on a fiber of its own.
 *
 * A scheduled method takes no parameters and returns `IO[Unit]`. Its fiber waits with `IO.sleep`, so it holds no thread
 * between runs, and starts with the `IOLocal` values of the start of the context. A failing run is logged and the schedule goes
 * on. The fibers start when every bean of the context exists and are cancelled in the [[dev.coilix.context.ClosingPhase.Producers]]
 * phase of closing, before any bean is destroyed.
 *
 * @param environment
 *   resolves placeholders in the durations of the annotations
 * @param fibers
 *   the fibers of the scheduled methods that are running
 */
@Slf4s
case class ScheduledMethodRegistrar(environment: Option[Environment], fibers: Ref[IO, List[FiberIO[Unit]]])
    extends ContextLifecycleListener {

  /**
   * Starts the annotated methods of every bean in `context`.
   *
   * The start fails with [[dev.coilix.concurrency.InvalidScheduledMethodException]] if a method takes parameters, does not
   * return `IO`, is static, cannot be made accessible, or has a missing or invalid duration.
   */
  override def onContextStarted(context: ApplicationContext): IO[Unit] =
    IO.blocking(context.createdBeans.flatMap(bean => methodsOf(bean.beanName, ProxiedBean.unwrap(bean.instance))))
      .flatMap(_.traverse(started))
      .flatMap(started => fibers.update(_ ++ started))

  override def closingPhase: ClosingPhase = ClosingPhase.Producers

  override def onContextClosing: IO[Unit] =
    fibers.getAndSet(Nil).flatMap(_.traverse_(_.cancel))

  /** The number of scheduled methods that run. */
  def activeTasks: IO[Int] = fibers.get.map(_.size)

  private def methodsOf(beanName: String, bean: AnyRef): List[ScheduledMethod] =
    hierarchy(bean.getClass)
      .flatMap(_.getDeclaredMethods.toList)
      .filter(method => !method.isBridge && !method.isSynthetic && method.isAnnotationPresent(classOf[Scheduled]))
      .distinctBy(_.getName)
      .sortBy(_.getName)
      .map(method => scheduledMethod(beanName, bean, method))

  private def hierarchy(beanClass: Class[?]): List[Class[?]] =
    List.unfold(Option[Class[?]](beanClass))(_.map(current => (current, Option(current.getSuperclass))))

  private def scheduledMethod(beanName: String, bean: AnyRef, method: Method): ScheduledMethod = {
    val label = s"@Scheduled method ${method.getName} of bean '$beanName'"
    problemOf(method).foreach(problem => throw InvalidScheduledMethodException(s"$label $problem"))
    ScheduledMethod(label, bean, method, trigger(label, method.getAnnotation(classOf[Scheduled])))
  }

  private def problemOf(method: Method): Option[String] =
    List(
      Modifier.isStatic(method.getModifiers)                 -> "must not be static",
      (method.getParameterCount > 0)                         -> "must not take parameters",
      !classOf[IO[?]].isAssignableFrom(method.getReturnType) -> s"must return IO[Unit], not ${method.getReturnType.getName}",
      !method.trySetAccessible()                             ->
        s"cannot be made accessible, open the package ${method.getDeclaringClass.getPackageName} to Coilix"
    ).collectFirst { case (true, problem) => problem }

  private def trigger(label: String, annotation: Scheduled): ScheduledTrigger = {
    val initialDelay = duration(label, "initialDelay", annotation.initialDelay)
    (annotation.fixedRate.trim, annotation.fixedDelay.trim) match {
      case ("", "")    => throw InvalidScheduledMethodException(s"$label sets neither fixedRate nor fixedDelay")
      case (rate, "")  => ScheduledTrigger.FixedRate(initialDelay, duration(label, "fixedRate", rate))
      case ("", delay) => ScheduledTrigger.FixedDelay(initialDelay, duration(label, "fixedDelay", delay))
      case _           => throw InvalidScheduledMethodException(s"$label sets both fixedRate and fixedDelay, set only one")
    }
  }

  private def duration(label: String, attribute: String, raw: String): FiniteDuration =
    try {
      environment.getOrElse(Environment.empty).resolve(ValueExpression.parse(raw), classOf[FiniteDuration]) match {
        case value: FiniteDuration => value
        case other => throw IllegalArgumentException(s"expected a duration, got $other")
      }
    } catch {
      case NonFatal(failure) =>
        throw InvalidScheduledMethodException(s"$label has an invalid $attribute '$raw': ${failure.getMessage}", Some(failure))
    }

  private def started(method: ScheduledMethod): IO[FiberIO[Unit]] =
    logF.info(s"Scheduling ${method.label} ${method.trigger}") *> runs(method).start

  private def runs(method: ScheduledMethod): IO[Unit] =
    method.trigger match {
      case ScheduledTrigger.FixedRate(initialDelay, period) =>
        IO.sleep(initialDelay) *> IO.monotonic.flatMap(atFixedRate(method, period, _))
      case ScheduledTrigger.FixedDelay(initialDelay, delay) =>
        IO.sleep(initialDelay) *> (run(method) *> IO.sleep(delay)).foreverM
    }

  private def atFixedRate(method: ScheduledMethod, period: FiniteDuration, start: FiniteDuration): IO[Unit] =
    run(method) *> IO.monotonic.flatMap { now =>
      val next = (start + period).max(now)
      IO.sleep(next - now) *> atFixedRate(method, period, next)
    }

  private def run(method: ScheduledMethod): IO[Unit] =
    invoked(method).handleErrorWith(failure => logF.error(s"${method.label} failed, it runs again on schedule", failure))

  /** The effect of the method. The cast cannot fail, because only methods that return `IO` are scheduled. */
  @SuppressWarnings(Array("org.wartremover.warts.AsInstanceOf"))
  private def invoked(method: ScheduledMethod): IO[Unit] =
    IO.defer(method.method.invoke(method.bean).asInstanceOf[IO[Unit]]).adaptError {
      case invocation: InvocationTargetException => Option(invocation.getCause).getOrElse(invocation)
    }
}

/** Creation of the [[dev.coilix.concurrency.ScheduledMethodRegistrar]]. */
object ScheduledMethodRegistrar {

  /** A registrar without running methods. Creating the `Ref` runs no effect, so the registrar is a plain bean. */
  def apply(environment: Option[Environment]): ScheduledMethodRegistrar =
    ScheduledMethodRegistrar(environment, Ref.unsafe[IO, List[FiberIO[Unit]]](Nil))
}
