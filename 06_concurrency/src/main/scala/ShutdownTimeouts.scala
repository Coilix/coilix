package dev.coilix.concurrency

import cats.effect.IO
import cats.effect.Outcome
import dev.coilix.annotation.ShutdownTimeout
import dev.coilix.annotation.Slf4s
import dev.coilix.config.Environment
import dev.coilix.config.ValueExpression
import dev.coilix.context.BeanDefinition
import dev.coilix.context.MethodInstanceFactory
import scala.concurrent.duration.FiniteDuration

/**
 * Limits the finalizers of beans created by the [[dev.coilix.concurrency.IOEffectInterpreter]].
 *
 * A finalizer that takes longer than its limit is cancelled in the background and logged, and one that fails is logged, so
 * closing goes on with the other beans in both cases.
 *
 * @param settings
 *   gives the limit of a bean without [[dev.coilix.annotation.ShutdownTimeout]]
 * @param environment
 *   resolves placeholders in [[dev.coilix.annotation.ShutdownTimeout]]
 */
@Slf4s
final class ShutdownTimeouts(settings: CatsEffectSettings, environment: Option[Environment]) {

  /** The limit for the finalizer of the bean of `definition`. */
  def of(definition: BeanDefinition): FiniteDuration =
    annotated(definition).fold(settings.shutdownTimeout)(annotation => parsed(annotation.value))

  /** Runs `finalizer` of the bean of `definition` within its limit, logging a failure or a timeout instead of raising it. */
  def bounded(definition: BeanDefinition, finalizer: IO[Unit]): IO[Unit] = {
    val limit = of(definition)
    finalizer.start.flatMap(fiber => fiber.join.timeoutTo(limit, abandoned(definition, limit, fiber.cancel))).flatMap(reported(
      definition
    ))
  }

  private def annotated(definition: BeanDefinition): Option[ShutdownTimeout] =
    definition.factory match {
      case MethodInstanceFactory(method) => Option(method.getAnnotation(classOf[ShutdownTimeout]))
      case _                             => None
    }

  private def parsed(raw: String): FiniteDuration =
    environment.getOrElse(Environment.empty).resolve(ValueExpression.parse(raw), classOf[FiniteDuration]) match {
      case duration: FiniteDuration => duration
      case other => throw IllegalArgumentException(s"@ShutdownTimeout(\"$raw\") is not a duration: $other")
    }

  private def abandoned(
    definition: BeanDefinition,
    limit:      FiniteDuration,
    cancel:     IO[Unit]
  ): IO[Outcome[IO, Throwable, Unit]] =
    cancel.start *> IO(log.warn(s"Finalizer of bean '${definition.beanName}' took longer than $limit and was cancelled"))
      .as(Outcome.canceled[IO, Throwable, Unit])

  private def reported(definition: BeanDefinition)(outcome: Outcome[IO, Throwable, Unit]): IO[Unit] =
    outcome match {
      case Outcome.Errored(failure) => IO(log.warn(s"Finalizer of bean '${definition.beanName}' failed", failure))
      case _                        => IO.unit
    }
}
