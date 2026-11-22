package dev.coilix.events

import dev.coilix.common.GenericType
import dev.coilix.common.TypeInfo
import dev.coilix.events.listeners.Recorder
import dev.coilix.events.model.Changed
import dev.coilix.events.model.Customer
import munit.FunSuite

final class EventsTest extends FunSuite {

  test("publishes through the publisher in scope with the static type of the event") {
    val published        = Recorder[(Any, GenericType)]()
    given EventPublisher = new EventPublisher {
      override def publish[E: TypeInfo](event: E): Unit = published.record((event, TypeInfo[E].genericType))
    }
    Events.publish(Changed(Customer("Ann")))
    assertEquals(
      published.recorded,
      List((Changed(Customer("Ann")), GenericType.Concrete(classOf[Changed[?]], List(GenericType.of(classOf[Customer])))))
    )
  }
}
