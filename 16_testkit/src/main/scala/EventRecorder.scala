package dev.coilix.testkit

import cats.effect.IO
import dev.coilix.common.TypeInfo
import dev.coilix.events.ApplicationEventListener
import java.util.concurrent.atomic.AtomicReference

/**
 * Records every application event published in a test context, including the events of the framework, in the order they were
 * delivered.
 *
 * Every context built by the testkit has one, named `eventRecorder`. It is a synchronous listener, so an event is recorded before
 * `publish` returns. Tests read it through [[dev.coilix.testkit.CoilixContextOps.recordedEvents]]:
 *
 * ```scala
 * setup.use {
 *   bean[Checkout].place(order)
 *   assertEquals(recordedEvents[OrderPlaced], List(OrderPlaced(order.id, order.customer)))
 * }
 * ```
 *
 * @param recorded
 *   the recorded events, oldest first
 */
case class EventRecorder(recorded: AtomicReference[List[AnyRef]]) extends ApplicationEventListener[AnyRef] {

  override def onEvent(event: AnyRef): IO[Unit] =
    IO(recorded.updateAndGet(_ :+ event)).void

  /** Every recorded event, oldest first. */
  def events: List[AnyRef] = recorded.get

  /**
   * The recorded events whose class is `E` or a subclass of it, oldest first. Type arguments are not compared, so
   * `eventsOf[Changed[Customer]]` also returns a `Changed[Product]`.
   */
  def eventsOf[E: TypeInfo]: List[E] =
    recorded.get.collect { case event if TypeInfo[E].isInstance(event) => TypeInfo[E].runtimeClass.cast(event) }

  /** Forgets the recorded events, for example after the startup events of the context. */
  def clear(): Unit = recorded.set(Nil)
}

/** Factories of [[dev.coilix.testkit.EventRecorder]]. */
object EventRecorder {

  /** A recorder with no events. */
  def apply(): EventRecorder = EventRecorder(AtomicReference(List.empty[AnyRef]))
}
