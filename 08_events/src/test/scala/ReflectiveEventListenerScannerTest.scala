package dev.coilix.events

import dev.coilix.common.GenericType
import dev.coilix.events.invalid.GenericListener
import dev.coilix.events.invalid.LambdaListener
import dev.coilix.events.invalid.Overloaded
import dev.coilix.events.invalid.ReturnsValue
import dev.coilix.events.invalid.TwoParameters
import dev.coilix.events.listeners.OrderAudit
import dev.coilix.events.listeners.OrderJournal
import dev.coilix.events.model.Customer
import dev.coilix.events.model.OrderEvent
import munit.FunSuite

final class ReflectiveEventListenerScannerTest extends FunSuite {
  private val scanner = ReflectiveEventListenerScanner(EventsSettings())

  test("finds a private annotated method and calls it with an event") {
    val journal  = OrderJournal()
    val listener = found("orderJournal", journal).loneElement
    listener.invoke(OrderEvent.Placed(4))
    assertEquals(
      (listener.info.name, listener.info.eventType, journal.placed.recorded),
      ("orderJournal.onPlaced", GenericType.of(classOf[OrderEvent.Placed]), List(OrderEvent.Placed(4)))
    )
  }

  private def found(name: String, bean: AnyRef): List[RegisteredListener] =
    scanner.listenersOf(name, bean).fold(error => fail(error.message), identity)

  private def rejected(name: String, bean: AnyRef): String =
    scanner.listenersOf(name, bean).fold(_.message, listeners => fail(s"Expected an error, got $listeners"))

  test("reads the event type of an ApplicationEventListener from its class") {
    assertEquals(
      found("orderAudit", OrderAudit()).map(_.info.eventType),
      List(GenericType.of(classOf[OrderEvent]))
    )
  }

  test("rejects methods that cannot receive events") {
    val messages = List("twoParameters" -> TwoParameters(), "returnsValue" -> ReturnsValue(), "overloaded" -> Overloaded())
      .map { case (name, bean) => rejected(name, bean) }
    assertEquals(
      messages.map(_.takeWhile(_ != ',')),
      List(
        "@EventListener method onPlaced of bean 'twoParameters' must take exactly one parameter",
        "@EventListener method onPlaced of bean 'returnsValue' must return Unit",
        "@EventListener methods of bean 'overloaded' are overloaded as on. Give each listener method its own name"
      )
    )
  }

  test("returns the problem of a listener as a typed error") {
    assertEquals(scanner.listenersOf("overloaded", Overloaded()), Left(EventListenerError.OverloadedMethod("overloaded", "on")))
  }

  test("rejects listeners whose event type is not known at runtime") {
    val messages = List("generic" -> GenericListener[Customer](), "lambda" -> LambdaListener.listener)
      .map { case (name, bean) => rejected(name, bean) }
    assertEquals(
      messages.map(_.takeWhile(_ != '.')),
      List(
        "@EventListener method onAny of bean 'generic' receives events of type T, which is not known at runtime",
        "ApplicationEventListener bean 'lambda' does not declare the type of its events"
      )
    )
  }

  extension [T](values: List[T]) {
    private def loneElement: T =
      values match {
        case single :: Nil => single
        case other         => fail(s"expected one element, got $other")
      }
  }
}
