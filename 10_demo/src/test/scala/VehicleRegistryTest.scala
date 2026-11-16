package dev.coilix.demo

import dev.coilix.demo.TestWorkshop.golfVin
import dev.coilix.demo.TestWorkshop.octaviaVin
import dev.coilix.demo.access.AccessError
import dev.coilix.demo.access.OemKeyRegistry
import dev.coilix.demo.access.PublicKey
import dev.coilix.demo.vehicle.Action
import dev.coilix.demo.vehicle.Vin
import munit.FunSuite

final class VehicleRegistryTest extends FunSuite {
  private val registry = TestWorkshop.registry()

  test("connects every configured vehicle with its modules") {
    assertEquals(registry.all.map(_.model), List("Golf", "Octavia"))
    assertEquals(registry.find(octaviaVin).map(_.modules.keySet), Some(Set(TestWorkshop.engine, TestWorkshop.abs)))
    assertEquals(
      registry.find(golfVin).flatMap(_.modules.get(TestWorkshop.immobilizer)).map(_.supportedActions),
      Some(Set(Action.ReadFaults, Action.Code))
    )
  }

  test("does not know other vehicles") {
    assertEquals(registry.find(Vin("WBA00000000000003")), None)
  }

  test("the OEM registry trusts the workshop key for configured vehicles") {
    val keys = OemKeyRegistry(TestWorkshop.properties, TestWorkshop.keys)
    assertEquals(registry.find(golfVin).map(keys.getKey), Some(Right(PublicKey(TestWorkshop.keys.getPublic))))
    assertEquals(
      OemKeyRegistry(TestWorkshop.properties.copy(vehicles = Nil), TestWorkshop.keys).getKey(TestWorkshop.vehicle(golfVin)),
      Left(AccessError.UnknownVehicle)
    )
  }
}
