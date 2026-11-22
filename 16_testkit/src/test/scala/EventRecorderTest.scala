package dev.coilix.testkit

import dev.coilix.testkit.fixture.Visit
import munit.FunSuite

final class EventRecorderTest extends FunSuite {

  test("returns the events of one class in the order they were recorded") {
    val recorder = EventRecorder()
    recorder.onEvent(Visit("Ann"))
    recorder.onEvent("not a visit")
    recorder.onEvent(Visit("Bob"))
    assertEquals((recorder.eventsOf[Visit], recorder.events.size), (List(Visit("Ann"), Visit("Bob")), 3))
  }

  test("forgets the events after clear") {
    val recorder = EventRecorder()
    recorder.onEvent(Visit("Ann"))
    recorder.clear()
    assertEquals(recorder.events, Nil)
  }
}
