package dev.coilix.demo

import dev.coilix.demo.TestWorkshop.engine
import dev.coilix.demo.TestWorkshop.golfVin
import dev.coilix.demo.TestWorkshop.octaviaVin
import dev.coilix.demo.access.AccessError
import dev.coilix.demo.access.ClaimsCodec
import dev.coilix.demo.access.Ed25519TokenVerifier
import dev.coilix.demo.access.Grant
import dev.coilix.demo.access.PublicKey
import dev.coilix.demo.vehicle.Action
import munit.FunSuite

final class TokenSigningTest extends FunSuite {
  private val key      = PublicKey(TestWorkshop.keys.getPublic)
  private val verifier = Ed25519TokenVerifier()

  test("verifies a token signed by the workshop") {
    assertEquals(verifier.verify(TestWorkshop.token(golfVin, Grant(engine, Action.ReadFaults)), key), Right(()))
  }

  test("refuses changed claims and malformed signatures") {
    val token = TestWorkshop.token(golfVin, Grant(engine, Action.ReadFaults))
    assertEquals(
      verifier.verify(token.copy(claims = token.claims.copy(vehicle = octaviaVin)), key),
      Left(AccessError.InvalidSignature)
    )
    assertEquals(verifier.verify(token.copy(signature = Vector(1, 2, 3)), key), Left(AccessError.InvalidSignature))
  }

  test("encodes grants in a stable order") {
    val forward  = TestWorkshop.claims(golfVin, Grant(engine, Action.ReadFaults), Grant(engine, Action.Adapt))
    val backward = TestWorkshop.claims(golfVin, Grant(engine, Action.Adapt), Grant(engine, Action.ReadFaults))
    assertEquals(ClaimsCodec.encode(forward).toList, ClaimsCodec.encode(backward).toList)
  }
}
