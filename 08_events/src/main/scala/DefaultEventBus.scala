package dev.coilix.events

import cats.data.EitherT
import cats.effect.Deferred
import cats.effect.IO
import cats.effect.IOLocal
import cats.effect.Ref
import cats.effect.Resource
import cats.effect.std.Supervisor
import cats.syntax.all.*
import dev.coilix.annotation.Slf4s
import dev.coilix.common.GenericType
import dev.coilix.common.TypeInfo
import dev.coilix.concurrency.RetryPolicy
import dev.coilix.context.ApplicationContext
import dev.coilix.context.ClosingPhase
import dev.coilix.context.ContextLifecycleListener
import dev.coilix.context.ProxiedBean
import fs2.concurrent.SignallingRef
import java.util.Objects
import scala.concurrent.duration.DurationInt
import scala.concurrent.duration.FiniteDuration

/**
 * The [[dev.coilix.events.EventBus]] registered by the events module as the `eventBus` bean, which keeps events only in memory.
 *
 * Synchronous listeners run one after another in the fiber of the publisher. Each asynchronous delivery is a fiber under the
 * `Supervisor` of the bus, which starts with the `IOLocal` values of the publisher, so it keeps the MDC of `coilix-logging`, and
 * a retry waits with `IO.sleep`. Events published before the context has started wait and are delivered when the listeners are
 * found. When the context closes, waiting retries end at once, and the bus waits for the running deliveries up to
 * `shutdownTimeout`, then cancels them and logs at `ERROR` level how many deliveries of each listener were abandoned. With
 * `coilix.events.synchronous = true` asynchronous listeners run in the fiber of the publisher too, after the synchronous ones,
 * and their retries follow at once.
 *
 * @param settings
 *   the closing timeout and the retry policies of listeners
 * @param status
 *   the stage of the bus with the events published before the start
 * @param registered
 *   the listeners found when the context started
 * @param pending
 *   the number of running or retrying asynchronous deliveries of each listener
 * @param abandonedRetries
 *   the number of retries of each listener that the closing ended
 * @param insideListener
 *   whether the current fiber runs a listener, which may publish while the context closes
 * @param supervisor
 *   runs the asynchronous deliveries
 * @param closing
 *   completed when the context starts closing, which ends the waiting retries
 * @param abandon
 *   completed when the closing stops waiting, which cancels the running deliveries
 */
@Slf4s
case class DefaultEventBus(
  settings:         EventsSettings,
  status:           Ref[IO, EventBusStatus],
  registered:       Ref[IO, List[RegisteredListener]],
  pending:          SignallingRef[IO, Map[String, Int]],
  abandonedRetries: Ref[IO, Map[String, Int]],
  insideListener:   IOLocal[Boolean],
  supervisor:       Supervisor[IO],
  closing:          Deferred[IO, Unit],
  abandon:          Deferred[IO, Unit]
) extends EventBus
    with ContextLifecycleListener {

  private val scanner = ReflectiveEventListenerScanner(settings)

  private val cancelGrace = 1.second

  override def tryPublish[E: TypeInfo](event: E): EitherT[IO, PublishError, Unit] =
    EitherT(IO(Objects.requireNonNull(boxed(event), "event")).flatMap(value =>
      published(value, typeOf(value, TypeInfo[E].genericType))
    ))

  override def listeners: IO[List[EventListenerInfo]] = registered.get.map(_.map(_.info))

  override def pendingDeliveries: IO[Int] = pending.get.map(_.values.sum)

  override def awaitIdle(timeout: FiniteDuration): IO[Boolean] =
    pending.discrete.find(_.isEmpty).compile.drain.as(true).timeoutTo(timeout, IO.pure(false))

  /**
   * Finds the listeners in every bean of `context` and delivers the events published before the start.
   *
   * The bus is the top layer of the check of listeners, so an [[dev.coilix.events.EventListenerError]] fails the start with
   * its [[dev.coilix.events.InvalidEventListenerException]].
   */
  override def onContextStarted(context: ApplicationContext): IO[Unit] =
    IO.blocking(context.createdBeans.flatTraverse(bean => listenersOf(bean.beanName, bean.instance)))
      .flatMap(found => IO.fromEither(found.left.map(_.exception)))
      .flatMap(started)

  override def closingPhase: ClosingPhase = ClosingPhase.Events

  override def onContextClosing: IO[Unit] =
    for {
      dropped  <- status.modify(current => (EventBusStatus(EventBusState.Closing, Nil), current.early))
      _        <- logF.warn(s"Dropping ${dropped.size} event(s) published before a context that did not start").whenA(dropped.nonEmpty)
      _        <- closing.complete(())
      finished <- awaitIdle(settings.shutdownTimeout)
      _        <- abandonedRetries.get.flatMap(logAbandoned("waiting for a retry", _))
      _        <- status.update(_.copy(state = EventBusState.Closed))
      _        <- cancelRunning.unlessA(finished)
    } yield ()

  private def started(found: List[RegisteredListener]): IO[Unit] =
    registered.set(found.sortBy(_.info.name)) *>
      logListeners(found.map(_.info).sortBy(_.name)) *>
      status.modify(current => (EventBusStatus(EventBusState.Running, Nil), current.early)).flatMap(_.traverse_(deliverEarly))

  private def published(event: AnyRef, eventType: GenericType): IO[Either[PublishError, Unit]] =
    status.modify(keptOrOpen(event, eventType)).flatMap {
      case None        => IO.pure(Right(()))
      case Some(state) =>
        refusal(state, eventType).flatMap(_.fold(deliver(event, eventType).map(Right(_)))(error => IO.pure(Left(error))))
    }

  private def keptOrOpen(event: AnyRef, eventType: GenericType)(current: EventBusStatus): (EventBusStatus, Option[EventBusState]) =
    if (current.state == EventBusState.Starting) (current.copy(early = current.early :+ EarlyEvent(event, eventType)), None)
    else (current, Some(current.state))

  private def refusal(state: EventBusState, eventType: GenericType): IO[Option[PublishError]] =
    state match {
      case EventBusState.Closing => insideListener.get.map(inside => Option.unless(inside)(PublishError.Closing(eventType)))
      case EventBusState.Closed  => IO.pure(Some(PublishError.Closed(eventType)))
      case _                     => IO.pure(None)
    }

  /** `event` as an object. The cast cannot fail, because the JVM passes every value of a type parameter as an object. */
  @SuppressWarnings(Array("org.wartremover.warts.AsInstanceOf"))
  private def boxed(event: Any): AnyRef = event.asInstanceOf[AnyRef]

  private def typeOf(event: AnyRef, staticType: GenericType): GenericType =
    if (event.getClass.getTypeParameters.nonEmpty || staticType.erasure == event.getClass) staticType
    else GenericType.of(event.getClass)

  private def deliver(event: AnyRef, eventType: GenericType): IO[Unit] =
    registered.get.flatMap { all =>
      val (async, sync) = all.filter(_.info.eventType.isAssignableFrom(eventType)).partition(_.info.async)
      logF.debug(s"No listener receives events of type $eventType").whenA(async.isEmpty && sync.isEmpty) *>
        sync.traverse(listener => asListener(listener.invoke(event)).attempt).flatMap { results =>
          combined(results.flatMap(_.left.toOption)).fold(async.traverse_(startAsync(_, event)))(IO.raiseError)
        }
    }

  private def combined(failures: List[Throwable]): Option[Throwable] =
    failures match {
      case Nil           => None
      case first :: rest =>
        rest.foreach(first.addSuppressed)
        Some(first)
    }

  private def asListener(effect: IO[Unit]): IO[Unit] =
    insideListener.getAndSet(true).flatMap(previous => effect.guarantee(insideListener.set(previous)))

  private def startAsync(listener: RegisteredListener, event: AnyRef): IO[Unit] =
    if (settings.synchronous) runInline(listener, event, 1)
    else
      increment(listener.info.name) *>
        supervisor.supervise(delivery(listener, event, 1).race(abandon.get).void.guarantee(decrement(listener.info.name))).void

  private def runInline(listener: RegisteredListener, event: AnyRef, attempt: Int): IO[Unit] =
    asListener(listener.invoke(event)).handleErrorWith(failure => retriedInline(listener, event, attempt, failure))

  private def retriedInline(listener: RegisteredListener, event: AnyRef, attempt: Int, failure: Throwable): IO[Unit] =
    if (listener.info.retry.delayAfter(attempt).isDefined)
      logF.warn(s"Event listener ${listener.info.name} failed on attempt $attempt, retrying at once", failure) *>
        runInline(listener, event, attempt + 1)
    else dropped(listener, event, attempt, failure)

  private def delivery(listener: RegisteredListener, event: AnyRef, attempt: Int): IO[Unit] =
    asListener(listener.invoke(event)).handleErrorWith(failure => retried(listener, event, attempt, failure))

  private def retried(listener: RegisteredListener, event: AnyRef, attempt: Int, failure: Throwable): IO[Unit] =
    retryDelay(listener.info.retry, attempt).flatMap {
      case Some(delay) => waitedForRetry(listener, event, attempt, failure, delay)
      case None        => dropped(listener, event, attempt, failure)
    }

  private def waitedForRetry(
    listener: RegisteredListener,
    event:    AnyRef,
    attempt:  Int,
    failure:  Throwable,
    delay:    FiniteDuration
  ): IO[Unit] =
    logF.warn(s"Event listener ${listener.info.name} failed on attempt $attempt, retrying in $delay", failure) *>
      IO.race(IO.sleep(delay), closing.get).flatMap {
        case Left(_)  => delivery(listener, event, attempt + 1)
        case Right(_) => abandonedRetries.update(counted(listener.info.name))
      }

  private def dropped(listener: RegisteredListener, event: AnyRef, attempt: Int, failure: Throwable): IO[Unit] =
    logF.error(s"Event listener ${listener.info.name} failed after $attempt attempt(s), dropping ${describe(event)}", failure)

  private def retryDelay(policy: RetryPolicy, attempt: Int): IO[Option[FiniteDuration]] =
    status.get.map(current => Option.when(current.state == EventBusState.Running)(policy.delayAfter(attempt)).flatten)

  private def cancelRunning: IO[Unit] =
    pending.get.flatMap(logAbandoned(s"running after ${settings.shutdownTimeout}", _)) *>
      abandon.complete(()) *>
      awaitIdle(cancelGrace).void

  private def logAbandoned(reason: String, counts: Map[String, Int]): IO[Unit] =
    logF
      .error(
        s"Closing abandoned asynchronous event deliveries $reason (${counts.toList.sorted.map((name, count) => s"$name: $count").mkString(", ")})"
      )
      .whenA(counts.nonEmpty)

  private def increment(name: String): IO[Unit] = pending.update(counted(name))

  private def decrement(name: String): IO[Unit] = pending.update(lowered(name))

  private def counted(name: String)(counts: Map[String, Int]): Map[String, Int] =
    counts.updated(name, counts.getOrElse(name, 0) + 1)

  private def lowered(name: String)(counts: Map[String, Int]): Map[String, Int] =
    counts.get(name) match {
      case Some(count) if count > 1 => counts.updated(name, count - 1)
      case _                        => counts - name
    }

  private def listenersOf(beanName: String, bean: AnyRef): Either[EventListenerError, List[RegisteredListener]] =
    ProxiedBean.unwrap(bean) match {
      case self if self eq this => Right(Nil)
      case target               => scanner.listenersOf(beanName, target)
    }

  private def logListeners(found: List[EventListenerInfo]): IO[Unit] =
    logF.info(s"Found ${found.size} event listener(s)") *> found.traverse_(logListener)

  private def logListener(info: EventListenerInfo): IO[Unit] =
    logF.debug(s"Event listener ${info.name} receives ${info.eventType}${if (info.async) " asynchronously" else ""}") *>
      logF
        .warn(s"Event listener ${info.name} has a retry policy, but only asynchronous listeners retry")
        .whenA(!info.async && info.retry != RetryPolicy.none)

  private def deliverEarly(early: EarlyEvent): IO[Unit] =
    deliver(early.event, early.eventType).handleErrorWith(failure =>
      logF.error(s"A listener failed on ${describe(early.event)} published before the context started", failure)
    )

  private def describe(event: AnyRef): String = s"an event of type ${event.getClass.getName}"
}

/** Creation of the [[dev.coilix.events.DefaultEventBus]]. */
object DefaultEventBus {

  /** A bus whose asynchronous deliveries still running when the resource is released are cancelled. */
  def resource(settings: EventsSettings): Resource[IO, DefaultEventBus] =
    Supervisor[IO](await = false).evalMap { supervisor =>
      (
        Ref.of[IO, EventBusStatus](EventBusStatus(EventBusState.Starting, Nil)),
        Ref.of[IO, List[RegisteredListener]](Nil),
        SignallingRef.of[IO, Map[String, Int]](Map.empty),
        Ref.of[IO, Map[String, Int]](Map.empty),
        IOLocal(false),
        Deferred[IO, Unit],
        Deferred[IO, Unit]
      ).mapN((status, registered, pending, abandoned, inside, closing, abandon) =>
        DefaultEventBus(settings, status, registered, pending, abandoned, inside, supervisor, closing, abandon)
      )
    }
}
