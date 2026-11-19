package dev.coilix.demo

import dev.coilix.demo.TestWorkshop.engine
import dev.coilix.demo.TestWorkshop.golfVin
import dev.coilix.demo.TestWorkshop.now
import dev.coilix.demo.access.AccessError
import dev.coilix.demo.access.CertificationAccessPolicy
import dev.coilix.demo.access.Grant
import dev.coilix.demo.access.GrantViolations
import dev.coilix.demo.access.WorkshopAccessSystem
import dev.coilix.demo.vehicle.Action
import dev.coilix.demo.vehicle.ModuleId
import munit.FunSuite
import scala.concurrent.duration.DurationInt

final class WorkshopAccessSystemTest extends FunSuite {
  private val golf   = TestWorkshop.vehicle(golfVin)
  private val system = WorkshopAccessSystem(CertificationAccessPolicy(), TestWorkshop.issuer, 5.minutes, 2)

  test("issues a token for a new session") {
    val requested = Set(Grant(engine, Action.ReadFaults))
    val claims    = system.authorize(TestWorkshop.master, golf, requested, now).map(_.claims)
    assertEquals(claims.map(_.grants), Right(requested))
    assertEquals(claims.map(_.vehicle), Right(golfVin))
    assertEquals(claims.map(_.expiresAt), Right(now.plusSeconds(300)))
  }

  test("validates the request before asking the policy") {
    val unknown = Set(Grant(ModuleId("radio"), Action.ReadFaults))
    val tooMany = Set(Action.ReadFaults, Action.ReadLiveData, Action.Adapt).map(Grant(engine, _))
    assertEquals(
      system.authorize(TestWorkshop.apprentice, golf, Set.empty, now),
      Left(AccessError.InvalidRequest(List(GrantViolations.noGrants)))
    )
    assertEquals(
      system.authorize(TestWorkshop.master, golf, unknown, now),
      Left(AccessError.InvalidRequest(List(GrantViolations.unknownModule)))
    )
    assertEquals(
      system.authorize(TestWorkshop.master, golf, tooMany, now),
      Left(AccessError.InvalidRequest(List(GrantViolations.tooManyGrants(2))))
    )
  }

  test("refuses what the policy refuses") {
    assertEquals(
      system.authorize(TestWorkshop.apprentice, golf, Set(Grant(engine, Action.Code)), now),
      Left(AccessError.NotCertified)
    )
  }
}
