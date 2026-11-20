package dev.coilix.concurrency

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
import java.util.concurrent.atomic.AtomicReference
import scala.concurrent.duration.FiniteDuration
import scala.util.control.NonFatal

/**
 * Runs the methods annotated with [[dev.coilix.annotation.Scheduled]] on the [[dev.coilix.concurrency.TaskScheduler]].
 *
 * When the context has created every bean, it finds the annotated methods of each bean, including private methods and methods
 * of superclasses, and schedules them. Durations are resolved like `@Value` expressions. Each scheduled method is logged at
 * `INFO` level. Scheduled methods bring new work in, so every task is cancelled in the
 * [[dev.coilix.context.ClosingPhase.Producers]] phase, before queued events are delivered and before any bean is destroyed.
 *
 * @param scheduler
 *   runs the methods
 * @param environment
 *   resolves placeholders in durations, or `None` to accept only literal durations
 */
@Slf4s
final class ScheduledMethodRegistrar(scheduler: TaskScheduler, environment: Option[Environment]) extends ContextLifecycleListener {

  private val tasks = AtomicReference(List.empty[ScheduledTask])

  /**
   * Schedules the annotated methods of every bean in `context`.
   *
   * @throws dev.coilix.concurrency.InvalidScheduledMethodException
   *   if a method takes parameters, is static, cannot be made accessible, or has a missing or invalid duration
   */
  override def onContextStarted(context: ApplicationContext): Unit = {
    val scheduled = context.beanNames.flatMap(name =>
      context.getBeanByName(name).toList.flatMap(bean => methodsOf(name, ProxiedBean.unwrap(bean)))
    )
    val _         = tasks.updateAndGet(_ ++ scheduled.map(schedule))
  }

  override def closingPhase: ClosingPhase = ClosingPhase.Producers

  override def onContextClosing(): Unit =
    tasks.getAndSet(Nil).foreach(_.cancel())

  /** The number of scheduled methods that are not cancelled yet. */
  def activeTasks: Int = tasks.get.count(!_.isCancelled)

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
      Modifier.isStatic(method.getModifiers) -> "must not be static",
      (method.getParameterCount > 0)         -> "must not take parameters",
      !method.trySetAccessible()             ->
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

  private def schedule(method: ScheduledMethod): ScheduledTask = {
    log.info(s"Scheduling ${method.label} ${method.trigger}")
    method.trigger match {
      case ScheduledTrigger.FixedRate(initialDelay, period) => scheduler.scheduleAtFixedRate(initialDelay, period)(run(method))
      case ScheduledTrigger.FixedDelay(initialDelay, delay) => scheduler.scheduleWithFixedDelay(initialDelay, delay)(run(method))
    }
  }

  private def unwrapped(failure: Throwable): Throwable =
    failure match {
      case invocation: InvocationTargetException => Option(invocation.getCause).getOrElse(invocation)
      case other => other
    }

  private def run(method: ScheduledMethod): Unit =
    try {
      val _ = method.method.invoke(method.bean)
    } catch {
      case NonFatal(failure) => throw unwrapped(failure)
    }
}
