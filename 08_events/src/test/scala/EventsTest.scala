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
      override def tryPublish[E: TypeInfo](event: E): Either[PublishError, Unit] =
        Right(published.record((event, TypeInfo[E].genericType)))
    }
    Events.publish(Changed(Customer("Ann")))
    assertEquals(
      published.recorded,
      List((Changed(Customer("Ann")), GenericType.Concrete(classOf[Changed[?]], List(GenericType.of(classOf[Customer])))))
    )
  }

  test("tryPublish returns the error of the publisher in scope") {
    given EventPublisher = new EventPublisher {
      override def tryPublish[E: TypeInfo](event: E): Either[PublishError, Unit] =
        Left(PublishError.Closed(TypeInfo[E].genericType))
    }
    assertEquals(Events.tryPublish(Customer("Ann")), Left(PublishError.Closed(GenericType.of(classOf[Customer]))))
    assertEquals(
      intercept[EventBusClosedException](Events.publish(Customer("Ann"))).getMessage,
      s"Cannot publish an event of type ${classOf[Customer].getName}, the context is closed"
    )
  }
}
