package dev.coilix.events

import cats.effect.IO
import dev.coilix.annotation.Slf4s
import dev.coilix.common.GenericType
import dev.coilix.common.TypeInfo
import dev.coilix.concurrency.BlockingExecutor
import dev.coilix.concurrency.FatalErrors
import dev.coilix.concurrency.ManagedExecutor
import dev.coilix.concurrency.RetryPolicy
import dev.coilix.concurrency.TaskExecutor
import dev.coilix.concurrency.TaskScheduler
import dev.coilix.context.ApplicationContext
import dev.coilix.context.ClosingPhase
import dev.coilix.context.ContextLifecycleListener
import dev.coilix.context.ProxiedBean
import java.util.Objects
import java.util.concurrent.atomic.AtomicLong
import java.util.concurrent.atomic.AtomicReference
import scala.annotation.tailrec
import scala.concurrent.duration.FiniteDuration
import scala.util.control.NonFatal

/**
 * The [[dev.coilix.events.EventBus]] registered by the events module as the `eventBus` bean, which keeps events only in memory.
 *
 * Synchronous listeners run one after another in the thread of the publisher. Each asynchronous delivery is a separate task on
 * the [[dev.coilix.concurrency.TaskExecutor]], or on the [[dev.coilix.concurrency.BlockingExecutor]] for a listener with
 * `blocking = true`, and a retry waits on the [[dev.coilix.concurrency.TaskScheduler]]. Events published
 * before the context has started wait and are delivered when the listeners are found. When the context closes, waiting retries
 * are cancelled, and the bus waits for the running deliveries up to `shutdownTimeout`, then interrupts them and logs at `ERROR`
 * level how many deliveries of each listener were abandoned. A fatal error of an asynchronous listener ends its delivery without
 * retries and goes to the [[dev.coilix.concurrency.FatalErrorHandler]] of the executor. With `coilix.events.synchronous = true` asynchronous listeners run in
 * the thread of the publisher too, after the synchronous ones, and their retries follow at once.
 *
 * @param executor
 *   runs the asynchronous deliveries
 * @param blockingExecutor
 *   runs the asynchronous deliveries of listeners that block
 * @param scheduler
 *   waits before retries
 * @param settings
 *   the closing timeout and the retry policies of listeners
 */
@Slf4s
final class DefaultEventBus(
  executor:         TaskExecutor,
  blockingExecutor: BlockingExecutor,
  scheduler:        TaskScheduler,
  settings:         EventsSettings
) extends EventBus
    with ContextLifecycleListener {

  private val scanner        = ReflectiveEventListenerScanner(settings)
  private val state          = AtomicReference(EventBusState.Starting)
  private val stateLock      = new AnyRef
  private val idle           = new AnyRef
  private val registered     = AtomicReference(List.empty[RegisteredListener])
  private val early          = AtomicReference(List.empty[EarlyEvent])
  private val pending        = AtomicReference(Map.empty[String, Int])
  private val running        = AtomicReference(Set.empty[Thread])
  private val retries        = AtomicReference(Map.empty[Long, PendingRetry])
  private val retryIds       = AtomicLong()
  private val insideListener = ThreadLocal.withInitial[Boolean](() => false)

  override def publish[E: TypeInfo](event: E): Unit = {
    val value     = Objects.requireNonNull(boxed(event), "event")
    val eventType = typeOf(value, TypeInfo[E].genericType)
    if (!keptForStart(value, eventType)) {
      checkOpen(eventType)
      deliver(value, eventType, failure => throw failure)
    }
  }

  override def listeners: List[EventListenerInfo] = registered.get.map(_.info)

  override def pendingDeliveries: Int = pending.get.values.sum

  override def awaitIdle(timeout: FiniteDuration): Boolean =
    awaitUntil(System.nanoTime + timeout.toNanos)

  /**
   * Finds the listeners in every bean of `context` and delivers the events published before the start.
   *
   * @throws dev.coilix.events.InvalidEventListenerException
   *   if a listener cannot be called with events
   */
  override def onContextStarted(context: ApplicationContext): IO[Unit] = IO.blocking {
    val found = context.createdBeans.flatMap(bean => listenersOf(bean.beanName, bean.instance))
    registered.set(found.sortBy(_.info.name))
    logListeners(registered.get.map(_.info))
    stateLock.synchronized {
      state.set(EventBusState.Running)
      early.getAndSet(Nil)
    }.foreach(deliverEarly)
  }

  override def closingPhase: ClosingPhase = ClosingPhase.Events

  override def onContextClosing: IO[Unit] = IO.blocking {
    discardEarly()
    state.set(EventBusState.Closing)
    logAbandoned("waiting for a retry", cancelRetries())
    val finished = awaitIdle(settings.shutdownTimeout)
    state.set(EventBusState.Closed)
    if (!finished) interruptRunning()
  }

  /** `event` as an object. The cast cannot fail, because the JVM passes every value of a type parameter as an object. */
  @SuppressWarnings(Array("org.wartremover.warts.AsInstanceOf"))
  private def boxed(event: Any): AnyRef = event.asInstanceOf[AnyRef]

  private def typeOf(event: AnyRef, staticType: GenericType): GenericType =
    if (event.getClass.getTypeParameters.nonEmpty || staticType.erasure == event.getClass) staticType
    else GenericType.of(event.getClass)

  private def keptForStart(event: AnyRef, eventType: GenericType): Boolean =
    stateLock.synchronized {
      val starting = state.get == EventBusState.Starting
      if (starting) {
        val _ = early.updateAndGet(_ :+ EarlyEvent(event, eventType))
      }
      starting
    }

  private def checkOpen(eventType: GenericType): Unit =
    state.get match {
      case EventBusState.Closing if !insideListener.get =>
        throw EventBusClosedException(s"Cannot publish an event of type $eventType, the context is closing")
      case EventBusState.Closed                         =>
        throw EventBusClosedException(s"Cannot publish an event of type $eventType, the context is closed")
      case _                                            => ()
    }

  private def deliver(event: AnyRef, eventType: GenericType, onFailure: Throwable => Unit): Unit = {
    val (async, sync) = registered.get.filter(_.info.eventType.isAssignableFrom(eventType)).partition(_.info.async)
    if (async.isEmpty && sync.isEmpty) log.debug(s"No listener receives events of type $eventType")
    combined(sync.flatMap(listener => failureOf(listener, event))) match {
      case Some(failure) => onFailure(failure)
      case None          => async.foreach(listener => startAsyncListener(listener, event))
    }
  }

  private def startAsyncListener(listener: RegisteredListener, event: AnyRef): Unit =
    if (settings.synchronous) runInline(listener, event, 1) else startAsync(listener, event, 1)

  private def runInline(listener: RegisteredListener, event: AnyRef, attempt: Int): Unit =
    failureOf(listener, event).foreach(failure => retriedInline(listener, event, attempt, failure))

  private def retriedInline(listener: RegisteredListener, event: AnyRef, attempt: Int, failure: Throwable): Unit =
    if (listener.info.retry.delayAfter(attempt).isDefined) {
      log.warn(s"Event listener ${listener.info.name} failed on attempt $attempt, retrying at once", failure)
      runInline(listener, event, attempt + 1)
    } else {
      log.error(s"Event listener ${listener.info.name} failed after $attempt attempt(s), dropping ${describe(event)}", failure)
    }

  private def failureOf(listener: RegisteredListener, event: AnyRef): Option[Throwable] =
    try {
      asListener(listener.invoke(event))
      None
    } catch {
      case NonFatal(failure) => Some(failure)
    }

  private def combined(failures: List[Throwable]): Option[Throwable] =
    failures match {
      case Nil           => None
      case first :: rest =>
        rest.foreach(first.addSuppressed)
        Some(first)
    }

  private def asListener(body: => Unit): Unit = {
    val previous = insideListener.get
    insideListener.set(true)
    try {
      body
    } finally {
      insideListener.set(previous)
    }
  }

  private def startAsync(listener: RegisteredListener, event: AnyRef, attempt: Int): Unit = {
    increment(listener.info.name)
    submit(listener, event, attempt)
  }

  private def submit(listener: RegisteredListener, event: AnyRef, attempt: Int): Unit =
    try {
      executorOf(listener).execute(runAsync(listener, event, attempt))
    } catch {
      case NonFatal(failure) =>
        log.error(s"Event listener ${listener.info.name} could not be started", failure)
        decrement(listener.info.name)
    }

  private def executorOf(listener: RegisteredListener): ManagedExecutor =
    if (listener.info.blocking) blockingExecutor else executor

  private def runAsync(listener: RegisteredListener, event: AnyRef, attempt: Int): Unit = {
    val thread = Thread.currentThread
    val _      = running.updateAndGet(_ + thread)
    val retry  =
      try {
        asListener(listener.invoke(event))
        false
      } catch {
        case NonFatal(failure) => retried(listener, event, attempt, failure)
        case interrupted: InterruptedException =>
          thread.interrupt()
          log.warn(s"Event listener ${listener.info.name} was interrupted", interrupted)
          false
        case fatal if FatalErrors.isFatal(fatal) => abandoned(listener, fatal)
      } finally {
        val _ = running.updateAndGet(_ - thread)
      }
    if (!retry) decrement(listener.info.name)
  }

  private def abandoned(listener: RegisteredListener, fatal: Throwable): Nothing = {
    decrement(listener.info.name)
    throw fatal
  }

  private def retried(listener: RegisteredListener, event: AnyRef, attempt: Int, failure: Throwable): Boolean =
    retryDelay(listener.info.retry, attempt) match {
      case Some(delay) if scheduleRetry(listener, event, attempt + 1, delay) =>
        log.warn(s"Event listener ${listener.info.name} failed on attempt $attempt, retrying in $delay", failure)
        true
      case _                                                                 =>
        log.error(s"Event listener ${listener.info.name} failed after $attempt attempt(s), dropping ${describe(event)}", failure)
        false
    }

  private def retryDelay(policy: RetryPolicy, attempt: Int): Option[FiniteDuration] =
    if (state.get == EventBusState.Running) policy.delayAfter(attempt) else None

  private def scheduleRetry(listener: RegisteredListener, event: AnyRef, attempt: Int, delay: FiniteDuration): Boolean = {
    val id = retryIds.incrementAndGet()
    val _  = retries.updateAndGet(_.updated(id, PendingRetry(listener.info.name, None)))
    if (state.get == EventBusState.Running) scheduled(id, listener, event, attempt, delay) else !removed(id)
  }

  private def scheduled(id: Long, listener: RegisteredListener, event: AnyRef, attempt: Int, delay: FiniteDuration): Boolean =
    try {
      val task = scheduler.schedule(delay)(runRetry(id, listener, event, attempt))
      val _    = retries.updateAndGet(pending => pending.get(id).fold(pending)(retry => pending.updated(id, retry.scheduled(task))))
      true
    } catch {
      case NonFatal(failure) =>
        log.error(s"A retry of event listener ${listener.info.name} could not be scheduled", failure)
        !removed(id)
    }

  private def runRetry(id: Long, listener: RegisteredListener, event: AnyRef, attempt: Int): Unit =
    if (removed(id)) submit(listener, event, attempt)

  private def removed(id: Long): Boolean = retries.getAndUpdate(_ - id).contains(id)

  private def cancelRetries(): Map[String, Int] = {
    val cancelled = retries.getAndSet(Map.empty).values.toList
    cancelled.foreach { retry =>
      retry.task.foreach(_.cancel())
      decrement(retry.listener)
    }
    cancelled.groupMapReduce(_.listener)(_ => 1)(_ + _)
  }

  private def interruptRunning(): Unit = {
    logAbandoned(s"running after ${settings.shutdownTimeout}", pending.get)
    running.get.foreach(_.interrupt())
  }

  private def logAbandoned(reason: String, counts: Map[String, Int]): Unit =
    if (counts.nonEmpty) {
      val listed = counts.toList.sorted.map { case (name, count) => s"$name: $count" }.mkString(", ")
      log.error(s"Closing abandoned asynchronous event deliveries $reason ($listed)")
    }

  private def increment(name: String): Unit = {
    val _ = pending.updateAndGet(counts => counts.updated(name, counts.getOrElse(name, 0) + 1))
  }

  private def decrement(name: String): Unit =
    if (pending.updateAndGet(counts => lowered(counts, name)).isEmpty) {
      idle.synchronized(idle.notifyAll())
    }

  private def lowered(counts: Map[String, Int], name: String): Map[String, Int] =
    counts.get(name) match {
      case Some(count) if count > 1 => counts.updated(name, count - 1)
      case _                        => counts - name
    }

  @tailrec
  private def awaitUntil(deadline: Long): Boolean = {
    val remainingMillis = (deadline - System.nanoTime) / 1000000L
    if (pending.get.isEmpty) true
    else if (remainingMillis <= 0) false
    else {
      idle.synchronized {
        if (pending.get.nonEmpty) idle.wait(remainingMillis)
      }
      awaitUntil(deadline)
    }
  }

  private def listenersOf(beanName: String, bean: AnyRef): List[RegisteredListener] =
    ProxiedBean.unwrap(bean) match {
      case self if self eq this => Nil
      case target               => scanner.listenersOf(beanName, target)
    }

  private def logListeners(found: List[EventListenerInfo]): Unit = {
    log.info(s"Found ${found.size} event listener(s)")
    found.foreach { info =>
      log.debug(s"Event listener ${info.name} receives ${info.eventType}${mode(info)}")
      if (!info.async && info.retry != RetryPolicy.none) {
        log.warn(s"Event listener ${info.name} has a retry policy, but only asynchronous listeners retry")
      }
      if (!info.async && info.blocking) {
        log.warn(s"Event listener ${info.name} is marked blocking, but runs in the thread of the publisher because it is not async")
      }
    }
  }

  private def mode(info: EventListenerInfo): String =
    if (!info.async) "" else if (info.blocking) " asynchronously on the blocking executor" else " asynchronously"

  private def deliverEarly(early: EarlyEvent): Unit =
    deliver(
      early.event,
      early.eventType,
      failure => log.error(s"A listener failed on ${describe(early.event)} published before the context started", failure)
    )

  private def discardEarly(): Unit = {
    val dropped = early.getAndSet(Nil)
    if (dropped.nonEmpty) log.warn(s"Dropping ${dropped.size} event(s) published before a context that did not start")
  }

  private def describe(event: AnyRef): String = s"an event of type ${event.getClass.getName}"
}
