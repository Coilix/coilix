package dev.coilix.demo

import dev.coilix.demo.access.AccessError
import dev.coilix.demo.access.InMemoryReplayGuard
import dev.coilix.demo.access.SessionId
import dev.coilix.demo.vehicle.RequestId
import munit.FunSuite

final class InMemoryReplayGuardTest extends FunSuite {

  test("accepts each request once per session") {
    val guard = InMemoryReplayGuard()
    assertEquals(guard.claim(SessionId("S-1"), RequestId("R-1")), Right(()))
    assertEquals(guard.claim(SessionId("S-1"), RequestId("R-1")), Left(AccessError.ReplayedRequest))
    assertEquals(guard.claim(SessionId("S-2"), RequestId("R-1")), Right(()))
  }
}
