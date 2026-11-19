package dev.coilix.demo

import dev.coilix.demo.TestWorkshop.abs
import dev.coilix.demo.TestWorkshop.engine
import dev.coilix.demo.TestWorkshop.golfVin
import dev.coilix.demo.TestWorkshop.now
import dev.coilix.demo.TestWorkshop.octaviaVin
import dev.coilix.demo.access.AccessError
import dev.coilix.demo.access.AuthToken
import dev.coilix.demo.access.Ed25519TokenIssuer
import dev.coilix.demo.access.Grant
import dev.coilix.demo.vehicle.Action
import dev.coilix.demo.vehicle.ModuleCommand
import dev.coilix.demo.vehicle.ModuleId
import dev.coilix.demo.vehicle.ModuleResult
import dev.coilix.demo.vehicle.RequestId
import java.security.KeyPairGenerator
import munit.FunSuite

final class VehicleTest extends FunSuite {
  private val readFaults = Grant(engine, Action.ReadFaults)

  private def command(action: Action, request: String = "R-1"): ModuleCommand = ModuleCommand(action, RequestId(request))

  test("runs a granted command") {
    val golf = TestWorkshop.vehicle(golfVin)
    assertEquals(
      golf.program(TestWorkshop.token(golfVin, readFaults), engine, command(Action.ReadFaults), now),
      Right(ModuleResult(Map("faults" -> "P0301")))
    )
  }

  test("refuses a token signed by another workshop") {
    val rogue = Ed25519TokenIssuer(KeyPairGenerator.getInstance("Ed25519").generateKeyPair())
    val token = rogue.issue(TestWorkshop.claims(golfVin, readFaults)).getOrElse(fail("signing failed"))
    assertEquals(
      TestWorkshop.vehicle(golfVin).program(token, engine, command(Action.ReadFaults), now),
      Left(AccessError.InvalidSignature)
    )
  }

  test("refuses a token with a grant added after signing") {
    val token: AuthToken = TestWorkshop.token(golfVin, readFaults)
    val widened = token.copy(claims = token.claims.copy(grants = token.claims.grants + Grant(engine, Action.Code)))
    assertEquals(
      TestWorkshop.vehicle(golfVin).program(widened, engine, command(Action.Code), now),
      Left(AccessError.InvalidSignature)
    )
  }

  test("refuses a token for another vehicle") {
    assertEquals(
      TestWorkshop.vehicle(octaviaVin).program(TestWorkshop.token(golfVin, readFaults), engine, command(Action.ReadFaults), now),
      Left(AccessError.WrongVehicle)
    )
  }

  test("refuses an expired token") {
    val token = TestWorkshop.token(golfVin, readFaults)
    assertEquals(
      TestWorkshop.vehicle(golfVin).program(token, engine, command(Action.ReadFaults), token.claims.expiresAt),
      Left(AccessError.TokenExpired)
    )
  }

  test("refuses an action the token does not grant") {
    assertEquals(
      TestWorkshop.vehicle(golfVin).program(TestWorkshop.token(golfVin, readFaults), engine, command(Action.Adapt), now),
      Left(AccessError.MissingGrant)
    )
  }

  test("refuses a module the vehicle does not have") {
    val radio = ModuleId("radio")
    assertEquals(
      TestWorkshop.vehicle(golfVin).program(
        TestWorkshop.token(golfVin, Grant(radio, Action.ReadFaults)),
        radio,
        command(Action.ReadFaults),
        now
      ),
      Left(AccessError.UnknownModule)
    )
  }

  test("refuses an action the module does not support") {
    assertEquals(
      TestWorkshop.vehicle(golfVin).program(TestWorkshop.token(golfVin, Grant(abs, Action.Code)), abs, command(Action.Code), now),
      Left(AccessError.ModuleRejected)
    )
  }

  test("refuses a replayed request") {
    val golf  = TestWorkshop.vehicle(golfVin)
    val token = TestWorkshop.token(golfVin, readFaults)
    val _     = golf.program(token, engine, command(Action.ReadFaults), now)
    assertEquals(golf.program(token, engine, command(Action.ReadFaults), now), Left(AccessError.ReplayedRequest))
    assert(golf.program(token, engine, command(Action.ReadFaults, "R-2"), now).isRight)
  }
}
