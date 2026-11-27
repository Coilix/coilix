package dev.coilix.testkit

import cats.effect.unsafe.implicits.global
import dev.coilix.testkit.fixture.Visit
import munit.FunSuite

final class EventRecorderTest extends FunSuite {

  test("returns the events of one class in the order they were recorded") {
    val recorder = EventRecorder()
    recorder.onEvent(Visit("Ann")).unsafeRunSync()
    recorder.onEvent("not a visit").unsafeRunSync()
    recorder.onEvent(Visit("Bob")).unsafeRunSync()
    assertEquals((recorder.eventsOf[Visit], recorder.events.size), (List(Visit("Ann"), Visit("Bob")), 3))
  }

  test("forgets the events after clear") {
    val recorder = EventRecorder()
    recorder.onEvent(Visit("Ann")).unsafeRunSync()
    recorder.clear()
    assertEquals(recorder.events, Nil)
  }
}
