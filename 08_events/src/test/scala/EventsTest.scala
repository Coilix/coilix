package dev.coilix.events

import cats.data.EitherT
import cats.effect.IO
import dev.coilix.common.GenericType
import dev.coilix.common.TypeInfo
import dev.coilix.events.listeners.Recorder
import dev.coilix.events.model.Changed
import dev.coilix.events.model.Customer
import munit.CatsEffectSuite

final class EventsTest extends CatsEffectSuite {

  test("publishes through the publisher in scope with the static type of the event") {
    val published        = Recorder[(Any, GenericType)]()
    given EventPublisher = new EventPublisher {
      override def tryPublish[E: TypeInfo](event: E): EitherT[IO, PublishError, Unit] =
        EitherT.right(IO(published.record((event, TypeInfo[E].genericType))))
    }
    Events.publish(Changed(Customer("Ann"))).map { _ =>
      assertEquals(
        published.recorded,
        List((Changed(Customer("Ann")), GenericType.Concrete(classOf[Changed[?]], List(GenericType.of(classOf[Customer])))))
      )
    }
  }

  test("tryPublish returns the error of the publisher in scope, and publish fails with its exception") {
    given EventPublisher = new EventPublisher {
      override def tryPublish[E: TypeInfo](event: E): EitherT[IO, PublishError, Unit] =
        EitherT.leftT(PublishError.Closed(TypeInfo[E].genericType))
    }
    for {
      refused <- Events.tryPublish(Customer("Ann")).value
      failure <- Events.publish(Customer("Ann")).intercept[EventBusClosedException]
    } yield {
      assertEquals(refused, Left(PublishError.Closed(GenericType.of(classOf[Customer]))))
      assertEquals(failure.getMessage, s"Cannot publish an event of type ${classOf[Customer].getName}, the context is closed")
    }
  }
}
